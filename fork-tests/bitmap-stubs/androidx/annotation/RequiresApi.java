package androidx.annotation; import java.lang.annotation.*; @Target({ElementType.TYPE,ElementType.METHOD,ElementType.CONSTRUCTOR}) public @interface RequiresApi { int value(); }
