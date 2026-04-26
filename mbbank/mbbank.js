const puppeteer = require("puppeteer");
const axios = require("axios");
const fs = require("fs");
const path = require("path");
const moment = require("moment-timezone");
const { spawn } = require("child_process");

const TEMP_FILE = path.join(__dirname, "login_data.json");
const HISTORY_FILE = path.join(__dirname, "history.json");

// ================== Quản lý session ==================
function saveTempData(data) {
  fs.writeFileSync(TEMP_FILE, JSON.stringify(data, null, 2));
}
function loadTempData() {
  if (fs.existsSync(TEMP_FILE)) {
    return JSON.parse(fs.readFileSync(TEMP_FILE, "utf-8"));
  }
  return null;
}
function updateTempData(data) {
  tempData = data;
  saveTempData(tempData);
}

let tempData = loadTempData();
let isLoggingIn = false;
let loginPromise = null;
let captchaResolved = Promise.resolve();

async function safeLogin(username, password) {
  if (isLoggingIn) return loginPromise;
  isLoggingIn = true;

  loginPromise = login(username, password)
    .then((res) => {
      isLoggingIn = false;
      if (res) updateTempData({ loginResult: res });
      return res;
    })
    .catch(() => {
      isLoggingIn = false;
      return null;
    });

  return loginPromise;
}

// ================== Captcha Queue ==================
let captchaQueue = Promise.resolve();
function solveCaptchaQueued(base64Img) {
  captchaQueue = captchaQueue.then(() => solveCaptcha(base64Img));
  return captchaQueue;
}
async function solveCaptcha(base64Img) {
  return new Promise((resolve, reject) => {
    const py = spawn("python", ["captcha_solver.py"]);
    let result = "";
    let error = "";

    py.stdin.write(base64Img);
    py.stdin.end();

    py.stdout.on("data", (data) => (result += data.toString()));
    py.stderr.on("data", (data) => (error += data.toString()));

    py.on("close", (code) => {
      if (code === 0 && result.trim()) {
        console.log("✅ CAPTCHA:", result.trim());
        resolve(result.trim());
      } else {
        console.error("❌ Captcha solver error:", error || "No result");
        reject(new Error(error));
      }
    });
  });
}

// ================== Login ==================
async function login(username, password) {
  let browser, page;
  try {
    browser = await puppeteer.launch({
      headless: true,
      args: [
        "--no-sandbox",
        "--disable-setuid-sandbox",
        "--disable-web-security",
        "--disable-blink-features=AutomationControlled",
      ],
    });

    page = await browser.newPage();
    await page.setUserAgent(
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    );
    await page.evaluateOnNewDocument(() => {
      Object.defineProperty(navigator, "webdriver", { get: () => false });
    });

    const client = await page.target().createCDPSession();
    await client.send("Network.clearBrowserCookies");
    await client.send("Network.clearBrowserCache");

    await page.goto("https://online.mbbank.com.vn/pl/login", {
      waitUntil: "networkidle2",
    });
    await page.waitForSelector('img[src^="data:image/png;base64,"]', {
      timeout: 30000,
    });

    const captchaBase64 = await page.$eval(
      'img[src^="data:image/png;base64,"]',
      (img) => img.src.replace(/^data:image\/png;base64,/, "")
    );
    captchaResolved = solveCaptchaQueued(captchaBase64);

    const captchaSolution = await captchaResolved;

    await page.type("#user-id", username);
    await page.type("#new-password", password);
    await page.type(
      'input[placeholder="NHẬP MÃ KIỂM TRA"]',
      captchaSolution
    );

    let result;
    page.on("response", async (response) => {
      if (response.url().includes("/doLogin") && response.status() === 200) {
        result = await response.json();
      }
    });

    await page.click("#login-btn");
    await new Promise((r) => setTimeout(r, 2000));
    await browser.close();

    if (result?.result?.ok && result.result?.responseCode === "00") {
      return result;
    } else {
      console.warn("⚠️ Login thất bại hoặc session invalid");
      return null;
    }
  } catch (err) {
    console.error("❌ Lỗi login:", err.message);
    if (browser) await browser.close();
    return null;
  }
}

// ================== Lấy lịch sử giao dịch ==================
async function lsgd(username, password, account_number, retryCount = 0) {
  if (retryCount > 3) {
    console.error("❌ Quá số lần relogin, dừng lại.");
    return null;
  }

  await captchaResolved;
  let session = tempData?.loginResult || (await safeLogin(username, password));
  if (!session) {
    console.warn("⚠️ Login thất bại, không thể lấy lịch sử giao dịch");
    return null;
  }

  const time =
    moment.tz("Asia/Ho_Chi_Minh").format("YYYYMMDDHHmmss") + "00";

  try {
    const res = await axios.post(
      "https://online.mbbank.com.vn/api/retail-transactionms/transactionms/get-account-transaction-history",
      {
        accountNo: account_number,
        fromDate: moment().subtract(3, "days").format("DD/MM/YYYY"),
        toDate: moment().format("DD/MM/YYYY"),
        sessionId: session.sessionId,
        refNo: `${account_number}-${time}`,
        deviceIdCommon: session.cust.deviceId,
      },
      {
        headers: {
          app: "MB_WEB",
          Authorization:
            "Basic RU1CUkVUQUlMV0VCOlNEMjM0ZGZnMzQlI0BGR0AzNHNmc2RmNDU4NDNm",
          Deviceid: session.cust.deviceId,
          Host: "online.mbbank.com.vn",
          Origin: "https://online.mbbank.com.vn",
          Referer:
            "https://online.mbbank.com.vn/information-account/source-account",
          Refno: `${account_number}-${time}`,
          "User-Agent":
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
          "X-Request-Id": `${account_number}-${time}`,
          "elastic-apm-traceparent":
            "00-" +
            Math.random().toString(16).slice(2).padEnd(32, "0") +
            "-" +
            Math.random().toString(16).slice(2).padEnd(16, "0") +
            "-01",
          "Content-Type": "application/json",
        },
      }
    );

    if (!res.data.result.ok || res.data.result.responseCode !== "00") {
      console.warn(`⚠️ Session invalid, relogin và retry lần ${retryCount + 1}`);
      const newSession = await safeLogin(username, password);
      if (!newSession) return null;
      return await lsgd(username, password, account_number, retryCount + 1);
    }

    return res.data;
  } catch (err) {
    console.error(`❌ API error (retry ${retryCount + 1}):`, err.message);
    const newSession = await safeLogin(username, password);
    if (!newSession) return null;
    return await lsgd(username, password, account_number, retryCount + 1);
  }
}

// ================== Quản lý lịch sử ==================
function loadHistory() {
  if (fs.existsSync(HISTORY_FILE)) {
    return JSON.parse(fs.readFileSync(HISTORY_FILE, "utf-8"));
  }
  return [];
}
function saveHistory(history) {
  fs.writeFileSync(HISTORY_FILE, JSON.stringify(history, null, 2));
}

async function notifyServer(amount, description) {
  try {
    await axios.post("http://127.0.0.1:5000/api/internal/payment_confirmed", {
      amount: amount,
      description: description
    });
    console.log(`📡 Đã báo về Server: ${amount} - ${description}`);
  } catch (err) {
    console.error("❌ Lỗi báo về Server:", err.message);
  }
}

// ================== Print giao dịch mới ==================
function printNewTransactions(accountNumber, transactions, history) {
  let hasNew = false;

  transactions.forEach((tx) => {
    const txId = tx.transactionId || tx.refNo || "-";
    const txContent = tx.addDescription || "";
    const amountVal = Number(tx.creditAmount);

    if (amountVal > 0 && !history.some((h) => h.id === txId)) {
      history.push({ id: txId, content: txContent, amount: amountVal.toLocaleString() });
      hasNew = true;

      // Gửi thông báo cho API Server
      notifyServer(amountVal, txContent);

      const dateTime = tx.transactionDate || tx.postingDate || "-";
      const [date, time] = dateTime.includes(" ")
        ? dateTime.split(" ")
        : [dateTime, "-"];

      const amountIn = tx.creditAmount
        ? "+" + Number(tx.creditAmount).toLocaleString()
        : "";
      const amountOut = tx.debitAmount
        ? "-" + Number(tx.debitAmount).toLocaleString()
        : "";

      const idCol = (txId + "").padEnd(15, " ");
      const dateCol = (date + " " + time).padEnd(20, " ");
      const inCol = (amountIn || "").padEnd(15, " ");
      const outCol = (amountOut || "").padEnd(15, " ");
      const contentCol = txContent;

      console.log(`${idCol} | ${dateCol} | ${inCol} | ${outCol} | ${contentCol}`);
    }
  });

  if (hasNew) saveHistory(history);
  return hasNew;
}

// ================== Check giao dịch ==================
let history = loadHistory();
async function checkTransactions(username, password, accountNumber) {
  const data = await lsgd(username, password, accountNumber);
  if (data?.transactionHistoryList?.length) {
    const hasNew = printNewTransactions(
      accountNumber,
      data.transactionHistoryList,
      history
    );
    if (!hasNew) console.log("⏳ Không có giao dịch mới.");
  } else {
    console.log("⏳ Không có giao dịch mới hoặc session invalid.");
  }
}

// ================== Run ==================
(async () => {
  const username = ""; // tài khoản đăng nhập
  const password = "";  // mật khẩu đăng nhập
  const accountNumber = ""; // số tài khoản

  console.log("🚀 Bắt đầu theo dõi giao dịch...");
  setInterval(() => checkTransactions(username, password, accountNumber), 5000);
})();
