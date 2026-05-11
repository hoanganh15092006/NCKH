import requests

urls = {
    "web_app.jpg": "https://images.unsplash.com/photo-1551288049-bebda4e38f71?q=80&w=800&auto=format&fit=crop", # Data Dashboard
    "web_ai.jpg": "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?q=80&w=800&auto=format&fit=crop", # AI concept
    "web_camera.jpg": "https://images.unsplash.com/photo-1557597774-9d273605dfa9?q=80&w=800&auto=format&fit=crop" # Tech/Camera concept
}

for filename, url in urls.items():
    try:
        response = requests.get(url, stream=True, timeout=10)
        if response.status_code == 200:
            with open(filename, 'wb') as f:
                for chunk in response.iter_content(1024):
                    f.write(chunk)
            print(f"Downloaded: {filename}")
    except Exception as e:
        print(f"Failed {filename}: {e}")
