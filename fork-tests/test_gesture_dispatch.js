const fs=require('fs'),path=require('path'),cp=require('child_process');const root=path.resolve(__dirname,'..');const s=fs.readFileSync(path.join(root,'TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java'),'utf8');
function method(sig){let a=s.indexOf(sig);if(a<0)throw Error(sig);let b=s.indexOf('{',a),n=0;do{if(s[b]==='{')n++;if(s[b]==='}')n--;b++;}while(n);return s.slice(a,b);}
for(const [name,id]of Object.entries({OPTION_DELETE:1,OPTION_FORWARD:2,OPTION_COPY:3,OPTION_SAVE_TO_GALLERY:4,OPTION_SAVE_TO_GALLERY2:7,OPTION_REPLY:8,OPTION_SAVE_TO_DOWNLOADS_OR_MUSIC:10,OPTION_EDIT:12,OPTION_PIN:13,OPTION_UNPIN:14}))if(!new RegExp(name+' = '+id+';').test(s))throw Error('Native registry ID drift: '+name);
for(const x of ['forkHandleGesture(view, x - view.getX(), y - view.getY(), "double_tap", true)','createDeleteMessagesAlert(selectedObject, selectedObjectGroup, true)','final int a = forkMenuIndices.get(position)','forkMenuPlan.moreIndices','scrimPopupWindowItems[a] == null','forkSendDoubleTapReaction(view, messageObject, x - view.getX(), y - view.getY())','forkGestureDispatching = true','final String forkAction = forkRequestedAction','if (nativeOption == OPTION_DELETE && selectedObject != null && showWelcomeMessageRevertOption(selectedObject)) nativeOption = -1','moreScroll.addView(forkMorePage)','if (forkHiddenIndices.contains(a)) forkMorePage.addView(cell)'])if(!s.includes(x))throw Error('Integration safeguard missing: '+x);
const source=method('private boolean forkPlainMessageBody(')+'\n'+method('private boolean forkHandleGesture(');
const fixture=`import java.util.*;import org.telegram.messenger.fork.actions.*;
public class GestureDispatchTest {
 static class View{float getX(){return 15;}float getY(){return 100;}}
 static class ClickableSpan{} interface Spanned extends CharSequence{<T>T[] getSpans(int a,int b,Class<T> c);}
 static class LinkedText implements Spanned{public int length(){return 4;}public char charAt(int i){return 'x';}public CharSequence subSequence(int a,int b){return "xx";}public <T>T[]getSpans(int a,int b,Class<T> c){return (T[])new ClickableSpan[]{new ClickableSpan()};}}
 static class TLRPC{static class TL_messageMediaEmpty{}}
 static class Owner{Object media,reply_markup;ArrayList<Object> entities=new ArrayList<>();}
 static class MessageObject{static final int TYPE_TEXT=0;int type,textWidth=100,currentAccount;CharSequence messageText="body";Owner messageOwner=new Owner();boolean sponsored,isDateObject;boolean isSending(){return false;}boolean isEditing(){return false;}boolean isSponsored(){return sponsored;}int textHeight(){return 40;}}
 static class ChatMessageCell extends View{MessageObject m=new MessageObject();MessageObject getMessageObject(){return m;}MessageObject getPrimaryMessageObject(){return m;}boolean isInsideBackground(float x,float y){return x>=0&&x<200;}int getTextX(){return 10;}int getTextY(){return 20;}}
 static class Bar{boolean mode;boolean isActionModeShowed(){return mode;}}
 Bar actionBar=new Bar();boolean searching,inPreviewMode,selection,trying,forkGestureDispatching;String forkRequestedAction;Object selectedObject,selectedObjectGroup,selectedObjectToEditCaption;int currentAccount,lastOption=-1,reactions,unavailable;float reactionX,reactionY;
 Object getParentActivity(){return this;}boolean isReport(){return false;}boolean hasTextSelection(){return selection;}boolean isTryingTextSelection(){return trying;}
 List<Integer> allowed=new ArrayList<>();boolean reentered;
 boolean forkSendDoubleTapReaction(View v,MessageObject m,float x,float y){reactions++;reactionX=x;reactionY=y;return true;}
 void forkShowActionUnavailable(){unavailable++;}
 boolean lastSingle,lastListView,lastLongpress,failMenu;
 boolean createMenu(View v,boolean single,boolean listView,float x,float y,boolean longpress){
  lastOption=ActionRegistry.availableOption(forkRequestedAction,allowed);lastSingle=single;lastListView=listView;lastLongpress=longpress;
  if(failMenu){failMenu=false;return false;}
  if(forkRequestedAction!=null){selectedObject=((ChatMessageCell)v).m;if(lastOption<0)unavailable++;forkRequestedAction=null;}
  if(reentered)return true;reentered=true;boolean nested=forkHandleGesture(v,x,y,"tap",true);if(!nested)throw new AssertionError("Reentrant gesture executed");return true;}
 ${source}
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
 GestureDispatchTest t=new GestureDispatchTest();ChatMessageCell v=new ChatMessageCell();
 check(!t.forkPlainMessageBody(v,15,5)&&!t.forkPlainMessageBody(v,15,70)&&t.forkPlainMessageBody(v,15,25),"Reply/footer/text bounds");
 v.m.messageText=new LinkedText();check(!t.forkPlainMessageBody(v,15,25),"Auto link intercepted");v.m.messageText="body";
 v.m.messageOwner.entities.add(new Object());check(!t.forkPlainMessageBody(v,15,25),"Entity intercepted");v.m.messageOwner.entities.clear();v.m.messageOwner.media=new Object();check(!t.forkPlainMessageBody(v,15,25),"Media intercepted");v.m.messageOwner.media=null;v.m.messageOwner.reply_markup=new Object();check(!t.forkPlainMessageBody(v,15,25),"Bot control intercepted");v.m.messageOwner.reply_markup=null;
 Owner savedOwner=v.m.messageOwner;v.m.messageOwner=null;check(!t.forkPlainMessageBody(v,15,25),"Missing message owner");v.m.messageOwner=savedOwner;
 ActionSettings.setGesture(0,"tap","copy");t.allowed.add(3);check(t.forkHandleGesture(v,15,25,"tap",true)&&t.lastOption==3,"Real native binding not dispatched");check(!t.lastSingle&&!t.lastListView&&!t.lastLongpress,"Explicit actions must build native option tuples");
 t.lastOption=-1;v.m.currentAccount=1;check(t.forkHandleGesture(v,15,25,"tap",true)&&t.lastOption==-1,"Wrong-account message executed");v.m.currentAccount=0;
 ActionSettings.setGesture(0,"swipe_right","copy");t.allowed.add(3);t.lastOption=-1;MessageObject savedMessage=v.m;v.m=null;check(t.forkHandleGesture(v,15,25,"swipe_right",false)&&t.lastOption==-1,"Recycled empty cell executed");v.m=savedMessage;
 ActionSettings.setGesture(0,"tap","copy");
 ActionSettings.setGesture(0,"long_press","copy");t.forkHandleGesture(v,15,25,"long_press",true);check(!t.lastSingle&&!t.lastListView&&!t.lastLongpress,"Long press explicit action flags");
 t.allowed.clear();ActionSettings.setGesture(0,"tap","copy");t.forkHandleGesture(v,15,25,"tap",true);check(t.lastOption==-1,"Unavailable action synthesized");
 t.actionBar.mode=true;check(!t.forkHandleGesture(v,15,25,"tap",true),"Selection intercepted");t.actionBar.mode=false;t.searching=true;check(!t.forkHandleGesture(v,15,25,"tap",true),"Search intercepted");t.searching=false;
 int before=t.reactions;ActionSettings.setGesture(0,"double_tap","reaction");t.forkHandleGesture(v,15,25,"double_tap",true);check(t.reactions==before+1&&t.reactionX==15&&t.reactionY==25&&!t.forkGestureDispatching,"Reaction dispatch must use local coordinates exactly once");
 ActionSettings.setGesture(0,"double_tap","none");check(t.forkHandleGesture(v,15,25,"double_tap",true)&&t.reactions==before+1,"Disabled double tap executes action");
 t.reentered=false;ActionSettings.setGesture(0,"tap","menu");t.forkHandleGesture(v,15,25,"tap",true);check(t.reentered,"Menu dispatch did not run");
 ActionSettings.setGesture(0,"tap","copy");int unavailableBefore=t.unavailable;t.failMenu=true;t.forkHandleGesture(v,15,25,"tap",true);check(t.unavailable==unavailableBefore+1,"Early menu exit must report unavailable instead of silently swallowing");

 check(ActionRegistry.availableOption("edit",Arrays.asList(1,3))==-1,"Edit permission bypass");check(ActionRegistry.needsConfirmation("delete")&&ActionRegistry.needsConfirmation("pin"),"Destructive metadata");
 check("native".equals(ActionSettings.gesture(1,"tap")),"Cross-account leak");org.telegram.messenger.UserConfig.setTestUserId(0,999);check("native".equals(ActionSettings.gesture(0,"tap")),"Reused account slot leaks settings");org.telegram.messenger.UserConfig.setTestUserId(0,100);check("copy".equals(ActionSettings.gesture(0,"tap")),"Settings not persisted");ActionSettings.resetGestures(0);check("native".equals(ActionSettings.gesture(0,"tap")),"Reset failed");
 System.out.println("GestureDispatchTest passed (production body/dispatch, native flags, reentrancy, fake Android host; no device claims)");}
}`;
const out=path.join(root,'build/actions-130-dispatch');fs.mkdirSync(out,{recursive:true});const f=path.join(out,'GestureDispatchTest.java');fs.writeFileSync(f,fixture);const home=process.env.JAVA_HOME||path.resolve(root,'../jdk17/jdk-17.0.20.1+1');const stubs=['android/content/Context.java','android/content/SharedPreferences.java','org/telegram/messenger/ApplicationLoader.java','org/telegram/messenger/UserConfig.java'].map(x=>path.join(root,'fork-tests/stubs',x));const base=path.join(root,'TMessagesProj/src/main/java/org/telegram/messenger/fork/actions');for(const [bin,args]of [['javac',['-encoding','UTF-8','-d',out,...stubs,path.join(base,'ActionRegistry.java'),path.join(base,'ActionSettings.java'),path.join(base,'ForkReactionSettings.java'),f]],['java',['-cp',out,'GestureDispatchTest']]]){const r=cp.spawnSync(path.join(home,'bin',bin+'.exe'),args,{stdio:'inherit'});if(r.error)throw r.error;if(r.status)process.exit(r.status);}
