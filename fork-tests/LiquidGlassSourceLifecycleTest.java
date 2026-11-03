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
  System.out.println("LiquidGlassSourceLifecycleTest passed (mock Android graphics)");
 }
}
