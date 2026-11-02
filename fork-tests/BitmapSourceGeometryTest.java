import android.graphics.Canvas;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceBitmap;
public class BitmapSourceGeometryTest {
 public static void main(String[] args) {
  BlurredBackgroundSourceBitmap source=new BlurredBackgroundSourceBitmap();
  Canvas first=source.beginRecording(120,160,2);
  if(first.bitmap.getWidth()!=60 || first.bitmap.getHeight()!=80) throw new AssertionError("Incorrect rectangular bitmap dimensions");
  if(first.scaleX!=0.5f || first.scaleY!=0.5f) throw new AssertionError("Recording must downscale the canvas");
  Canvas second=source.beginRecording(120,160,2);
  if(second.bitmap!=first.bitmap) throw new AssertionError("Same-sized active bitmap should be reused");
  source.endRecording();
  if(source.getBitmap()!=second.bitmap) throw new AssertionError("Recording bitmap was not published");
  System.out.println("BitmapSourceGeometryTest passed");
 }
}
