package android.content.pm;
public class SigningInfo { private final Signature[] values; public SigningInfo(Signature[] s){values=s;} public Signature[] getApkContentsSigners(){return values;} }
