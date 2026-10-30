package android.content.pm;
public class Signature { private final byte[] bytes; public Signature(byte[] b){bytes=b.clone();} public byte[] toByteArray(){return bytes.clone();} }
