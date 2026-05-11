import os
import requests
try:
    from duckduckgo_search import DDGS
except ImportError:
    import sys
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "duckduckgo-search"])
    from duckduckgo_search import DDGS

def download_image(query, filename):
    try:
        results = DDGS().images(query, max_results=5) # Get top 5, pick first working one
        for res in results:
            url = res.get('image')
            if not url: continue
            try:
                response = requests.get(url, stream=True, timeout=5)
                if response.status_code == 200:
                    with open(filename, 'wb') as f:
                        for chunk in response.iter_content(1024):
                            f.write(chunk)
                    print(f"Downloaded: {filename} from {url}")
                    return filename
            except:
                continue
    except Exception as e:
        print(f"Error downloading {query}: {e}")
    return None

download_image("smart parking lot barrier gate modern", "web_parking.jpg")
download_image("mobile banking app UI modern phone", "web_app.jpg")
download_image("artificial intelligence neural network abstract blue", "web_ai.jpg")
download_image("ALPR license plate recognition camera", "web_camera.jpg")
