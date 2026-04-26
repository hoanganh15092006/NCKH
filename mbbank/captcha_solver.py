import sys
import os
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

# Không dùng GPU
os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"
os.environ["CUDA_VISIBLE_DEVICES"] = ""

img_width = 320
img_height = 80
max_length = 6

characters_mbbank = [
    '2','3','4','5','6','7','8','9',
    'A','B','C','D','E','G','H','K','M','N','P','Q','U','V','Y','Z',
    'a','b','c','d','e','g','h','k','m','n','p','q','t','u','v','y','z'
]

char_to_num = layers.StringLookup(vocabulary=list(characters_mbbank), mask_token=None)
num_to_char = layers.StringLookup(vocabulary=char_to_num.get_vocabulary(), mask_token=None, invert=True)

def LoadModel(file):
    name, ext = os.path.splitext(file)
    if ext == ".json":
        with open(file, "r") as json_file:
            json_model = json_file.read()
        model = keras.models.model_from_json(json_model)
        model.load_weights(name + ".wgt")
    else:
        model = keras.models.load_model(file, custom_objects={'leaky_relu': tf.nn.leaky_relu})
    return model

# Load model 1 lần khi script chạy
model_mbbank = LoadModel("mbbank.json")

def decode_batch_predictions(pred):
    input_len = np.ones(pred.shape[0]) * pred.shape[1]
    results = keras.backend.ctc_decode(pred, input_length=input_len, greedy=True)[0][0][:, :max_length]
    output_text = []
    for res in results:
        res = tf.strings.reduce_join(num_to_char(res)).numpy().decode("utf-8")
        output_text.append(res)
    return output_text

def encode_base64x(b64_string):
    img = tf.io.decode_base64(b64_string)
    img = tf.io.decode_png(img, channels=1)
    img = tf.image.convert_image_dtype(img, tf.float32)
    img = tf.image.resize(img, [img_height, img_width])
    img = tf.transpose(img, perm=[1, 0, 2])
    return img

def solve_captcha(base64_str: str) -> str:
    imgstring = base64_str.replace("+", "-").replace("/", "_")
    image_encode = encode_base64x(imgstring)
    preds = model_mbbank.predict(np.array([image_encode]), verbose=0)  # 👈 tắt progress bar
    pred_texts = decode_batch_predictions(preds)
    captcha = pred_texts[0].replace('[UNK]', '').replace('-', '')
    return captcha

if __name__ == "__main__":
    base64_str = sys.stdin.read().strip()
    result = solve_captcha(base64_str)
    print(result, end="")
