import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.lang.reflect.Field;
import org.telegram.ui.Components.blur3.source.LiquidGlassBitmapSource;
public class LiquidGlassSourceLifecycleTest {
 public static void main(String[] args) throws Exception {
  LiquidGlassBitmapSource source=new LiquidGlassBitmapSource(); Bitmap bitmap=Bitmap.createBitmap(30,40,Bitmap.Config.ARGB_8888);
  source.setBitmap(bitmap); source.draw(new Canvas(bitmap),0,0,30,40);
  Field retained=LiquidGlassBitmapSource.class.getDeclaredField("shaderBitmap"); retained.setAccessible(true);
  if(retained.get(source)!=bitmap) throw new AssertionError("Fixture did not render");
  source.setBitmap(null);
  if(source.getBitmap()!=null || retained.get(source)!=null) throw new AssertionError("Bitmap still retained by shader");
  org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceBitmap original=new org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceBitmap();
  LiquidGlassBitmapSource linked=new LiquidGlassBitmapSource(original);
  original.setBitmap(bitmap);
  if(linked.getBitmap()!=bitmap || linked.getMatrix()!=original.getMatrix()) throw new AssertionError("Linked source did not follow original");
  Canvas software=new Canvas(bitmap); software.hardware=false; linked.draw(software,0,0,30,40);
  if(software.draws!=1) throw new AssertionError("Linked source did not fall back to the original bitmap");
  linked.draw(new Canvas(bitmap),0,0,30,40);
  original.setBitmap(null);
  linked.draw(new Canvas(bitmap),0,0,30,40);
  if(retained.get(linked)!=null) throw new AssertionError("Linked shader retained cleared bitmap");
  if(linked.getBitmap()!=null) throw new AssertionError("Linked source kept an outdated bitmap");
  System.out.println("LiquidGlassSourceLifecycleTest passed (mock Android graphics)");
 }
}
