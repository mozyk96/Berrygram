// Host regression for the real native menu gate, initialization, dispatch, and selected branches.
// Android UI/permission services are boundaries; this does not simulate touch delivery or claim device QA.
const fs = require('fs'), path = require('path'), cp = require('child_process');
const root = path.resolve(__dirname, '..');
const file = 'TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java';
const s = process.env.GESTURE_SOURCE_REF
    ? cp.execFileSync('git', ['show', `${process.env.GESTURE_SOURCE_REF}:${file}`], { cwd: root, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 }).replace(/\r\n/g, '\n')
    : fs.readFileSync(path.join(root, file), 'utf8').replace(/\r\n/g, '\n');
function block(start, offset = 0) {
    const a = s.indexOf(start, offset); if (a < 0) throw Error(start);
    let b = s.indexOf('{', a), depth = 0;
    do { if (s[b] === '{') depth++; if (s[b] === '}') depth--; b++; } while (depth);
    return s.slice(a, b);
}
function between(a, b, offset = 0) { const i = s.indexOf(a, offset); const j = s.indexOf(b, i); if (i < 0 || j < 0) throw Error(a); return s.slice(i, j); }
const gate = s.match(/if \(single \|\| (?:forkRequestedAction != null \|\| )?type < 2 \|\| type == 20\)/)[0];
const plainType = s.slice(s.indexOf('    private int getMessageType(')).match(/else if \(messageObject.isMediaEmpty\(\)\) \{\s*return 3;\s*\}/)[0].replace('else if', 'if');
const initialization = between('        selectedObject = null;\n        selectedObjectGroup = null;\n        forwardingMessage = null;', '        for (int a = 1; a >= 0; a--)');
const selection = between('                selectedObject = message;\n                selectedObjectGroup = groupedMessages;\n                fillMessageMenu(primaryMessage, icons, items, options);', '\n            }\n\n            if (selectedObject != null && selectedObject.isHiddenSensitive()');
const createMenuStart = s.indexOf('    private boolean createMenu(');
if (createMenuStart < 0) throw Error('createMenu');
const dispatch = block('            final String forkAction = forkRequestedAction;', createMenuStart);
const processStart = s.indexOf('    private void processSelectedOption(');
const forward = block('            case OPTION_FORWARD: {', processStart);
const reply = block('            case OPTION_REPLY: {', processStart);
// Execute the native ordinary-text copy fallback and UndoView initialization verbatim.
const copy = between('                    CharSequence caption = getMessageCaption(selectedObject, selectedObjectGroup);', '\n            case OPTION_SAVE_TO_GALLERY: {', processStart);
const cleanup = between('        selectedObject = null;\n        selectedObjectGroup = null;\n        selectedObjectToEditCaption = null;\n        closeMenu(!preserveDim);', '\n    }\n\n    public void showSuggestionOfferForEditMessage');
const source = `import java.util.*; import org.telegram.messenger.fork.actions.*;
public class GestureNativePathTest {
 static final int OPTION_REPLY=8,OPTION_FORWARD=2,OPTION_COPY=3,OPTION_DELETE=1;
 static class View {float getX(){return 15;} float getY(){return 100;}}
 static class Owner {boolean noforwards;}
 static class MessageObject {Owner messageOwner=new Owner(); int currentAccount;boolean isDateObject;boolean isMediaEmpty(){return true;}boolean isSending(){return false;}boolean isEditing(){return false;}boolean canEditMessage(Object c){return true;} long getGroupId(){return 0;} Object getFromPeer(){return null;} boolean isTodo(){return false;} boolean isPoll(){return false;} boolean isPublicPoll(){return false;} boolean isInvoice(){return false;} static class GroupedMessages{MessageObject captionMessage;}}
 static class ChatMessageCell extends View {MessageObject message=new MessageObject();MessageObject getMessageObject(){return message;}MessageObject getPrimaryMessageObject(){return message;}}
 static class Bar {boolean isActionModeShowed(){return false;}}
 static class Chat {long linked_monoforum_id;boolean megagroup;}
 static class ChatObject {static boolean isMonoForum(Chat c){return false;} static boolean canManageMonoForum(int a,Chat c){return false;} static boolean isNotInChat(Chat c){return false;} static boolean isChannel(Chat c){return false;} static boolean canPost(Chat c){return true;} static boolean canSendMessages(Chat c){return true;}}
 static class Bundle {Map<String,Object> values=new HashMap<>();void putBoolean(String k,boolean v){values.put(k,v);}void putInt(String k,int v){values.put(k,v);}void putLong(String k,long v){values.put(k,v);}}
 static class DialogsActivity {static final int DIALOGS_TYPE_FORWARD=3;Bundle args;DialogsActivity(Bundle a){args=a;}void setDelegate(Object o){}}
 static class DialogObject {static long getPeerDialogId(Object o){return 0;}}
 static class Config {long getClientUserId(){return 100;}}
 static class Controller {boolean isFrozen(){return false;}}
 static class AccountFrozenAlert {static void show(int a){}}
 static class UndoView {static final int ACTION_MESSAGE_COPIED=1;void showWithAction(int i,int a,Object o){}}
 static class AndroidUtilities {static CharSequence clipboard;static void addToClipboard(CharSequence s){clipboard=s;}}
 static class R {static class string {static final int ForkActionUnavailable=1;}}
 static class ForkLocale {static String get(int i){return "unavailable";}}
 static class BulletinFactory {GestureNativePathTest host;BulletinFactory(GestureNativePathTest h){host=h;}static BulletinFactory of(GestureNativePathTest h){return new BulletinFactory(h);}BulletinFactory createErrorBulletin(String s){return this;}void show(){host.unavailable++;}}
 Bar actionBar=new Bar(); boolean searching,inPreviewMode,forkGestureDispatching;String forkRequestedAction;int currentAccount;Chat currentChat;UndoView undoView;
 boolean forkSendDoubleTapReaction(View v,MessageObject m,float x,float y){return true;}
 void forkShowActionUnavailable(){unavailable++;}
 void forkShowActionUnavailable(String action){unavailable++;}
 MessageObject selectedObject,selectedObjectToEditCaption,forwardingMessage,replyingMessageObject,replied;MessageObject.GroupedMessages selectedObjectGroup,forwardingMessageGroup;
 int multiselect,unavailable,executions;DialogsActivity destination;boolean closed;
 interface Listener {void onDoubleTap(View v,int p,float x,float y);}Listener onItemClickListener=(v,p,x,y)->{};
 Object getParentActivity(){return this;}boolean isReport(){return false;}boolean hasTextSelection(){return false;}boolean isTryingTextSelection(){return false;}boolean forkPlainMessageBody(View v,float x,float y){return true;}
 Controller getMessagesController(){return new Controller();}Config getUserConfig(){return new Config();}long getDialogId(){return 10;}boolean isThreadChat(){return false;}MessageObject.GroupedMessages getGroup(long id){return null;}
 void presentFragment(DialogsActivity f){destination=f;}void showFieldPanelForReply(MessageObject m){replied=m;check(forkRequestedAction==null,"request escaped into native callback");check(forkHandleGesture(new ChatMessageCell(),20,25,"double_tap",true),"reentry must be consumed");}
 CharSequence getMessageCaption(MessageObject m,MessageObject.GroupedMessages g){return null;}CharSequence getMessageContent(MessageObject m,int a,boolean b){return "native text";}void createUndoView(){undoView=new UndoView();}void closeMenu(boolean b){closed=true;}
 boolean showWelcomeMessageRevertOption(MessageObject m){return false;}
 ArrayList<Integer> allowed=new ArrayList<>(Arrays.asList(8,2,3));
 void fillMessageMenu(MessageObject m,ArrayList<Integer> icons,ArrayList<CharSequence> items,ArrayList<Integer> options){options.addAll(allowed);}
 ${block('    private boolean forkHandleGesture(')}
 int getPlainMessageType(MessageObject messageObject){${plainType}return -1;}
 boolean createMenu(View v,boolean single,boolean listView,float x,float y,boolean longpress){
   MessageObject message=((ChatMessageCell)v).message,primaryMessage=message;MessageObject.GroupedMessages groupedMessages=null;
   // Native getMessageType returns 3 for ordinary media-empty sent text.
   int type=getPlainMessageType(message);
   ${initialization}
   ${gate} {
     ArrayList<Integer> icons=new ArrayList<>(),options=new ArrayList<>();ArrayList<CharSequence> items=new ArrayList<>();
     ${selection}
     ${dispatch}
     return true;
   }
   multiselect++;return false;
 }
 void processSelectedOption(int option){
   if(selectedObject==null||getParentActivity()==null)return;boolean preserveDim=false;executions++;
   switch(option){${forward} ${reply} case OPTION_COPY: { if (true) { ${copy}
   }
   ${cleanup}
 }
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[] args){
   GestureNativePathTest h=new GestureNativePathTest();ChatMessageCell cell=new ChatMessageCell();
   for(String action:Arrays.asList("reply","copy","forward")){
     String gesture=action.equals("reply")?"double_tap":action.equals("copy")?"long_press":"swipe_right";
     ActionSettings.setGesture(0,gesture,action);int before=h.executions;
     check(h.forkHandleGesture(cell,20,25,gesture,!action.equals("forward")),"gesture unhandled");
     check(h.multiselect==0,"custom "+action+" entered native multiselect instead of native options");
     check(h.executions==before+1,"native option must execute once");
     check(h.selectedObject==null&&h.selectedObjectGroup==null&&h.forkRequestedAction==null,"selected state leaked");
   }
   check(h.replied==cell.message,"reply target lost");check("native text".contentEquals(AndroidUtilities.clipboard),"native copy fallback failed");
   check(h.forwardingMessage==cell.message&&h.destination!=null&&Boolean.TRUE.equals(h.destination.args.values.get("onlySelect")),"native forward initialization lost");
   h.allowed.clear();int before=h.executions;h.forkHandleGesture(cell,20,25,"swipe_right",false);check(h.executions==before&&h.unavailable==1&&h.multiselect==0,"unavailable fell through");
   h.allowed.add(2);cell.message.currentAccount=1;h.forkHandleGesture(cell,20,25,"swipe_right",false);check(h.executions==before&&h.unavailable==1,"wrong-account message executed");cell.message.currentAccount=0;
   MessageObject saved=cell.message;cell.message=null;h.forkHandleGesture(cell,20,25,"swipe_right",false);check(h.executions==before,"recycled empty cell executed");cell.message=saved;
   ActionSettings.setGesture(0,"tap","native");check(!h.forkHandleGesture(cell,20,25,"tap",true),"native fallback consumed");
   ActionSettings.setGesture(0,"tap","none");check(h.forkHandleGesture(cell,20,25,"tap",true)&&h.executions==before,"none executed");
   System.out.println("GestureNativePathTest passed (extracted native gate/initialization/dispatch/reply/forward/plain-copy branches; Android service boundaries stubbed)");
 }
}`;
const out=path.join(root,'build/gesture-native-path');fs.mkdirSync(out,{recursive:true});const java=path.join(out,'GestureNativePathTest.java');fs.writeFileSync(java,source);
const home=process.env.JAVA_HOME||path.resolve(root,'../jdk17/jdk-17.0.20.1+1');
const stubs=['android/content/Context.java','android/content/SharedPreferences.java','org/telegram/messenger/ApplicationLoader.java','org/telegram/messenger/UserConfig.java'].map(x=>path.join(root,'fork-tests/stubs',x));
const base=path.join(root,'TMessagesProj/src/main/java/org/telegram/messenger/fork/actions');
for(const [bin,args]of [['javac',['-encoding','UTF-8','-d',out,...stubs,path.join(base,'ActionRegistry.java'),path.join(base,'ActionSettings.java'),java]],['java',['-cp',out,'GestureNativePathTest']]]){const r=cp.spawnSync(path.join(home,'bin',bin+'.exe'),args,{stdio:'inherit'});if(r.error)throw r.error;if(r.status)process.exit(r.status);}
