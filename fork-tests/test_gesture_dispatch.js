const fs=require('fs'),path=require('path'),cp=require('child_process');const root=path.resolve(__dirname,'..');const s=fs.readFileSync(path.join(root,'TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java'),'utf8');
function method(sig){let a=s.indexOf(sig);if(a<0)throw Error(sig);let b=s.indexOf('{',a),n=0;do{if(s[b]==='{')n++;if(s[b]==='}')n--;b++;}while(n);return s.slice(a,b);}
for(const [name,id]of Object.entries({OPTION_DELETE:1,OPTION_FORWARD:2,OPTION_COPY:3,OPTION_SAVE_TO_GALLERY:4,OPTION_SAVE_TO_GALLERY2:7,OPTION_REPLY:8,OPTION_SAVE_TO_DOWNLOADS_OR_MUSIC:10,OPTION_EDIT:12,OPTION_PIN:13,OPTION_UNPIN:14}))if(!new RegExp(name+' = '+id+';').test(s))throw Error('Native registry ID drift: '+name);
for(const x of ['forkHandleGesture(view, x - view.getX(), y - view.getY(), "double_tap", true)','createDeleteMessagesAlert(selectedObject, selectedObjectGroup, true)','final int a = forkMenuIndices.get(position)','forkMenuPlan.moreIndices','scrimPopupWindowItems[a] == null','messageObject.isDateObject || messageObject.isSending() || messageObject.isEditing()', 'if (nativeOption == OPTION_DELETE && selectedObject != null && showWelcomeMessageRevertOption(selectedObject)) nativeOption = -1', 'moreScroll.addView(forkMorePage)', 'if (forkHiddenIndices.contains(a)) forkMorePage.addView(cell)'])if(!s.includes(x))throw Error('Integration safeguard missing: '+x);
const source=method('private boolean forkPlainMessageBody(')+'\n'+method('private boolean forkHandleGesture(');
const fixture=`import java.util.*;import org.telegram.messenger.fork.actions.*;
public class GestureDispatchTest {
 static class View{float getX(){return 15;}float getY(){return 100;}}
 static class ClickableSpan{} interface Spanned extends CharSequence{<T>T[] getSpans(int a,int b,Class<T> c);}
 static class LinkedText implements Spanned{public int length(){return 4;}public char charAt(int i){return 'x';}public CharSequence subSequence(int a,int b){return "xx";}public <T>T[]getSpans(int a,int b,Class<T> c){return (T[])new ClickableSpan[]{new ClickableSpan()};}}
 static class TLRPC{static class TL_messageMediaEmpty{}}
 static class Owner{Object media,reply_markup;ArrayList<Object> entities=new ArrayList<>();}
 static class MessageObject{static final int TYPE_TEXT=0;int type,textWidth=100;CharSequence messageText="body";Owner messageOwner=new Owner();boolean sponsored;boolean isSponsored(){return sponsored;}int textHeight(){return 40;}}
 static class ChatMessageCell extends View{MessageObject m=new MessageObject();MessageObject getMessageObject(){return m;}boolean isInsideBackground(float x,float y){return x>=0&&x<200;}int getTextX(){return 10;}int getTextY(){return 20;}}
 static class Bar{boolean mode;boolean isActionModeShowed(){return mode;}}
 interface Listener{void onDoubleTap(View v,int p,float x,float y);}
 Bar actionBar=new Bar();boolean searching,inPreviewMode,selection,trying,forkExecutingReaction;String forkRequestedAction;int currentAccount,lastOption=-1,reactions;float reactionX,reactionY;
 Listener onItemClickListener=(v,p,x,y)->{reactions++;reactionX=x;reactionY=y;if(forkHandleGesture(v,x-v.getX(),y-v.getY(),"double_tap",true))throw new AssertionError("Reaction recursion");};
 Object getParentActivity(){return this;}boolean isReport(){return false;}boolean hasTextSelection(){return selection;}boolean isTryingTextSelection(){return trying;}
 List<Integer> allowed=new ArrayList<>();
 boolean createMenu(View v,boolean a,boolean b,float x,float y,boolean c){lastOption=ActionRegistry.availableOption(forkRequestedAction,allowed);return true;}
 ${source}
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
 GestureDispatchTest t=new GestureDispatchTest();ChatMessageCell v=new ChatMessageCell();
 check(!t.forkPlainMessageBody(v,15,5)&&!t.forkPlainMessageBody(v,15,70)&&t.forkPlainMessageBody(v,15,25),"Reply/footer/text bounds");
 v.m.messageText=new LinkedText();check(!t.forkPlainMessageBody(v,15,25),"Auto link intercepted");v.m.messageText="body";
 v.m.messageOwner.entities.add(new Object());check(!t.forkPlainMessageBody(v,15,25),"Entity intercepted");v.m.messageOwner.entities.clear();v.m.messageOwner.media=new Object();check(!t.forkPlainMessageBody(v,15,25),"Media intercepted");v.m.messageOwner.media=null;v.m.messageOwner.reply_markup=new Object();check(!t.forkPlainMessageBody(v,15,25),"Bot control intercepted");v.m.messageOwner.reply_markup=null;
 ActionSettings.setGesture(0,"tap","copy");t.allowed.add(3);check(t.forkHandleGesture(v,15,25,"tap",true)&&t.lastOption==3,"Real native binding not dispatched");t.allowed.clear();t.forkHandleGesture(v,15,25,"tap",true);check(t.lastOption==-1,"Unavailable action synthesized");
 t.actionBar.mode=true;check(!t.forkHandleGesture(v,15,25,"tap",true),"Selection intercepted");t.actionBar.mode=false;t.searching=true;check(!t.forkHandleGesture(v,15,25,"tap",true),"Search intercepted");t.searching=false;
 ActionSettings.setGesture(0,"double_tap","reaction");t.forkHandleGesture(v,15,25,"double_tap",true);check(t.reactions==1&&t.reactionX==30&&t.reactionY==125&&!t.forkExecutingReaction,"Reaction coordinate/recursion guard");
 ActionSettings.setGesture(0,"double_tap","none");check(t.forkHandleGesture(v,15,25,"double_tap",true)&&t.reactions==1,"Disabled double tap executes action");
 check(ActionRegistry.availableOption("edit",Arrays.asList(1,3))==-1,"Edit permission bypass");check(ActionRegistry.needsConfirmation("delete")&&ActionRegistry.needsConfirmation("pin"),"Destructive metadata");
 check("native".equals(ActionSettings.gesture(1,"tap")),"Cross-account leak");org.telegram.messenger.UserConfig.setTestUserId(0,999);check("native".equals(ActionSettings.gesture(0,"tap")),"Reused account slot leaks settings");org.telegram.messenger.UserConfig.setTestUserId(0,100);check("copy".equals(ActionSettings.gesture(0,"tap")),"Settings not persisted");ActionSettings.resetGestures(0);check("native".equals(ActionSettings.gesture(0,"tap")),"Reset failed");
 System.out.println("GestureDispatchTest passed (production body/dispatch, fake Android host; no device claims)");}
}`;
const out=path.join(root,'build/actions-130-dispatch');fs.mkdirSync(out,{recursive:true});const f=path.join(out,'GestureDispatchTest.java');fs.writeFileSync(f,fixture);const home=process.env.JAVA_HOME||path.resolve(root,'../jdk17/jdk-17.0.20.1+1');const stubs=['android/content/Context.java','android/content/SharedPreferences.java','org/telegram/messenger/ApplicationLoader.java','org/telegram/messenger/UserConfig.java'].map(x=>path.join(root,'fork-tests/stubs',x));const base=path.join(root,'TMessagesProj/src/main/java/org/telegram/messenger/fork/actions');for(const [bin,args]of [['javac',['-encoding','UTF-8','-d',out,...stubs,path.join(base,'ActionRegistry.java'),path.join(base,'ActionSettings.java'),f]],['java',['-cp',out,'GestureDispatchTest']]]){const r=cp.spawnSync(path.join(home,'bin',bin+'.exe'),args,{stdio:'inherit'});if(r.error)throw r.error;if(r.status)process.exit(r.status);}
