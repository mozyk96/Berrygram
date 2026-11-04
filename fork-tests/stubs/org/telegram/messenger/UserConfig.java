package org.telegram.messenger;
public class UserConfig {
 public static final int MAX_ACCOUNT_COUNT=4;
 private static final long[] ids={100,200,201,202};
 private final long id; private UserConfig(long id){this.id=id;}
 public static UserConfig getInstance(int account){return new UserConfig(ids[account]);}
 public static void setTestUserId(int account,long id){ids[account]=id;}
 public long getClientUserId(){return id;}
}
