from pptx import Presentation

def inspect_pptx(filepath):
    try:
        prs = Presentation(filepath)
        print(f"File: {filepath}")
        print(f"Number of slides: {len(prs.slides)}")
        
        print("\nAvailable Slide Layouts:")
        for i, layout in enumerate(prs.slide_layouts):
            print(f"Layout {i}: {layout.name}")
            
    except Exception as e:
        print(f"Error reading pptx: {e}")

if __name__ == "__main__":
    inspect_pptx(r"c:\Users\Admin\CAMNHANDIENBIENSO\NCKH\slide NCKH.pptx")
