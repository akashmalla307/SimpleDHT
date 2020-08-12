package edu.buffalo.cse.cse486586.simpledht;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    MatrixCursor cursor;
    MatrixCursor cursor1;
    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    String msgHash;

    static final int SERVER_PORT = 10000;
    //static final String TAG = SimpleDhtProvider.class.getSimpleName();
    HashMap<String, String> mssg = new HashMap<String, String>();
    HashMap<String, Integer> portsmap = new HashMap<String, Integer>();
    HashMap<String, String> currNodes = new HashMap<String, String>();
    HashMap<String, String> Nodes = new HashMap<String, String>();
    HashMap<String, String> querstarreply = new HashMap<String, String>();
    ArrayList<String> seq = new ArrayList<String>();
    ArrayList<String> avdseq = new ArrayList<String>();
    String queryvalue="null";

    String myportclient = null;
    String hash=null;
    String[] remote_ports;

    private final Uri mUri;
    int i1=0;
    String joining = "no";


    Comparator<HashMap<String, String>> cmp = new Comparator<HashMap<String, String>>() {
        @Override
        public int compare(HashMap<String, String> t1, HashMap<String, String> t2) {

            return t1.get("hash").compareTo(t2.get("hash"));
        }
    };

    // priority queue for storing all the mssg(hashmap) in agree seq wise (lowest agree seq will be on th top)
    PriorityQueue<HashMap<String, String> > pq = new
            PriorityQueue<HashMap<String, String>>(25, cmp);
    PriorityQueue<HashMap<String, String> > pqnew = new
            PriorityQueue<HashMap<String, String>>(25, cmp);
    Socket[] socketarray1 = new Socket[5];
    Socket[] socketarray2 = new Socket[5];
    public SimpleDhtProvider() {
        //mTextView = _tv;
        //mContentResolver = _cr;
        mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.SimpleDhtProvider");
        //   mContentValues = initTestValues();
        portsmap.put("11108",0);
        portsmap.put("11112",1);
        portsmap.put("11116",2);
        portsmap.put("11120",3);
        portsmap.put("11124",4);
        remote_ports  = new String[]{"11108", "11112", "11116", "11120","11124"};
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        Log.v(TAG,"Inside Delete"+uri +" "+selection+" "+selectionArgs);
        cursor = new MatrixCursor(new String[] { "key","value"});
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }
    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub

        Log.v(TAG,"insert query = " +values.toString()+" myportclient = "+myportclient);
        if(myportclient.equals(values.get("port")) && values.get("requesttype").equals("selfinsert")){
            Log.v(TAG,"received the insert request from Server to insert in its own cursor");
            Log.v("insert", values.toString());
            MatrixCursor.RowBuilder rowBuilder = cursor.newRow();
            rowBuilder.add("key",values.get("key")).add("value",values.get("value"));
           // Log.v("count=",String.valueOf(cursor.getCount()));
            return uri;
            //break;
        }else if(joining.equals("no")){
            Log.v(TAG,"Insert when joining value is NO");
            Log.v("insert", values.toString());
            //Log.v(TAG,"insert key "+values.get("key"));
            MatrixCursor.RowBuilder rowBuilder = cursor.newRow();
            rowBuilder.add("key",values.get("key")).add("value",values.get("value"));
            //Log.v("count=",String.valueOf(cursor.getCount()));
            return uri;
           // break;
        }else{
                Log.v(TAG,"insert Finding the correct AVD");
            try {
                msgHash = genHash((String) values.get("key"));
                Log.v(TAG, "hash value of key in insert = " + msgHash);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            ArrayList<String> arr = new ArrayList<String>();

            int count =0;
            for(String s : seq){
                arr.add(s);
                count++;
            }
            arr.add(msgHash);
            count++;
            Log.v(TAG,"count of avds+msgHash ="+count);
            Collections.sort(arr);

            for(String a: arr){
                Log.v(TAG,a);
            }
            int position=0;
            for(String a : arr){

                if(a.equals(msgHash))
                    break;

                position++;
            }
            Log.v(TAG,"position="+position);
            if(position == count-1){
                position =0;
            }

            String hash = seq.get(position);
            String avd = currNodes.get(hash);

            Log.v(TAG," key= "+values.get("key")+" value= "+values.get("value")+" hash of key= "+msgHash+ "sending to AVD = "+avd+" "+remote_ports[Integer.parseInt(avd)]);

            String requesttype = "insert";
            String msg = (String) values.get("value");
            String key = (String) values.get("key");
            String msgHash1 = msgHash;
            String myPort1 = remote_ports[Integer.parseInt(avd)];

            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, requesttype,msg, key,msgHash1,myPort1);


        }
       // return uri;
        return uri;
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        //mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        portsmap.put("11108",0);
        portsmap.put("11112",1);
        portsmap.put("11116",2);
        portsmap.put("11120",3);
        portsmap.put("11124",4);
        remote_ports  = new String[]{"11108", "11112", "11116", "11120","11124"};



        // TODO Auto-generated method stub
        cursor = new MatrixCursor(new String[] { "key","value"});

        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));


        myportclient = myPort;
        try {
            hash = genHash(portStr);
            Log.v(TAG, "my port =" + myPort + " " + portStr + " my hash value is = " + hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String msg = hash;

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
                    * port.
                    *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
                    * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);

    } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
                * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
        Log.e(TAG, "Can't create a ServerSocket");

        //return;
    }

        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);

            return true;
}
public String queryFun(String requesttype,String msgHash1,String myPort1,String key){
    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, requesttype,msgHash1,myPort1,key);
    queryvalue = "null";
    while(true){
        if(!queryvalue.equals("null")){
            return queryvalue;
        }
    }

}


    String ans = "null";
    public HashMap queryFun2(String requesttype){
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, requesttype);
        querstarreply = new HashMap<String, String>();
        ans="null";
        while(true){
            if(!ans.equals("null")){
                Log.v(TAG,"Got all the key n Value in HashMap");
                return querstarreply;
            }
        }
    }
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
        // TODO Auto-generated method stub

        Log.v(TAG,"Inside Query "+uri+" "+projection+" "+selection+" "+selectionArgs+" "+sortOrder);

        if(!selection.equals("null") && !selection.equals("@") && !selection.equals("*")){
            try {
                Log.v(TAG,"Finding the AVD for query key ="+selection);
                String hash = genHash(selection);

                ArrayList<String> arr = new ArrayList<String>();

                int count = 0;
                for (String s : seq) {
                    arr.add(s);
                    count++;
                }

                arr.add(hash);
                count++;
                Log.v(TAG, "count of avds+msgHash =" + count);
                Collections.sort(arr);

                for (String a : arr) {
                    Log.v(TAG, a);
                }
                int position = 0;
                for (String a : arr) {

                    if (a.equals(hash))
                        break;

                    position++;
                }
                Log.v(TAG, "position=" + position);
                if (position == count - 1) {
                    position = 0;
                }

                String hash1 = seq.get(position);
                String avd = currNodes.get(hash1);
                Log.v(TAG," hash of key= "+hash+ "sending to AVD = "+avd+" "+remote_ports[Integer.parseInt(avd)]);

                String requesttype = "query";
                String key = selection;
                String msgHash1 = hash;
                String myPort1 = remote_ports[Integer.parseInt(avd)];

                String value = queryFun(requesttype,msgHash1,myPort1,key);

                Log.v(TAG,"got the query reply="+value);
                MatrixCursor cursortmp = new MatrixCursor(new String[]{"key", "value"});
                MatrixCursor.RowBuilder rowBuilder1 = cursortmp.newRow();
                rowBuilder1.add("key", selection).add("value", value);
                return cursortmp;
            }catch(Exception e){
                e.printStackTrace();
            }
            }
        if(selection.equals("*")){
            Log.v(TAG,"going to return all the values (*)");
            try {
                String requesttype = "query*";

                if(seq.isEmpty()){
                    return cursor;
                }else {
                    HashMap<String, String> reply = queryFun2(requesttype);
                    Log.v(TAG,"reply="+reply);
                    MatrixCursor cursortmp1 = new MatrixCursor(new String[]{"key", "value"});
                    for (HashMap.Entry<String, String> entry : reply.entrySet()) {
                        //querstarreply.put(entry.getKey(),entry.getValue());
                        MatrixCursor.RowBuilder rowBuilder1 = cursortmp1.newRow();
                        rowBuilder1.add("key", entry.getKey()).add("value", entry.getValue());
                    }

                    return cursortmp1;
                }
            }catch(Exception e){
                e.printStackTrace();
            }

      }
        else if(selection.equals("@")){
            Log.v(TAG,"going to return all the values (@)");
            //Log.v(TAG,"cursor="+cursor);

            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    do {
                        int columnIndex = cursor.getColumnIndex("key");
                        String word = cursor.getString(columnIndex);
                        //Log.v("key=", word);

                        int columnIndex1 = cursor.getColumnIndex("value");
                        String word1 = cursor.getString(columnIndex1);
                        Log.v(TAG, " key= " + word + " value= " + word1);
                    }while(cursor.moveToNext());
                }
                cursor.close();
            }

           // return cursor;
        }


        else

        {
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex("key");
                    do {
                        String word = cursor.getString(columnIndex);
                        Log.v("key=", word);
                        if (word.equals(selection)) {
                            Log.v("key is found", "");
                            int columnIndex1 = cursor.getColumnIndex("value");
                            String word1 = cursor.getString(columnIndex1);
                            Log.v("value=", word1);
                            cursor1 = new MatrixCursor(new String[]{"key", "value"});
                            MatrixCursor.RowBuilder rowBuilder1 = cursor1.newRow();
                            rowBuilder1.add("key", word).add("value", word1);
                            break;
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            return cursor1;
        }
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }



    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {


        @SuppressLint("LongLogTag")
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            System.out.println("serverSocket=" + serverSocket);

            while (true) {

                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                    //socket.setSoTimeout(2500);
                    Log.v(TAG, "Inside Server code");

                    //http://www.jgyan.com/networking/how%20to%20send%20object%20over%20socket%20in%20java.php
                    ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
                    HashMap<String, String> hm1 = (HashMap<String, String>) is.readObject();
                    Log.v(TAG,"Inside Server hm1="+hm1);

                        try{
                           if (hm1.get("requesttype").equals("query*")){
                               Log.v(TAG,"Inside Query * in server");
                               String origin = hm1.get("origin");
                               HashMap<String, String> queryreply = new HashMap<String, String>();
                               if (cursor != null) {
                                   if (cursor.getCount() > 0) {
                                       cursor.moveToFirst();
                                       do {
                                           int columnIndex = cursor.getColumnIndex("key");
                                           String key = cursor.getString(columnIndex);
                                           int columnIndex1 = cursor.getColumnIndex("value");
                                           String value = cursor.getString(columnIndex1);
                                           queryreply.put(key, value);
                                       }while(cursor.moveToNext());
                                   }
                               }
                                       try {
                                           // Log.v(TAG, "Sending the reply now==" + fhmp);
                                           ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
                                           os.writeObject(queryreply);
                                           os.flush();
                                       } catch (Exception ex) {
                                           ex.printStackTrace();
                                       }

                               }
                            else if (hm1.get("requesttype").equals("query")) {
                                Log.v(TAG, "received query request=" + hm1);
                                String key = hm1.get("key");
                                String origin = hm1.get("origin");
                                String value ="";

                                if (cursor != null) {
                                    if (cursor.getCount() > 0) {
                                        cursor.moveToFirst();
                                        int columnIndex = cursor.getColumnIndex("key");
                                        do {
                                            String word = cursor.getString(columnIndex);
                                            Log.v("key=", word);
                                            if (word.equals(key)) {
                                                Log.v(TAG, "key is found");
                                                int columnIndex1 = cursor.getColumnIndex("value");
                                                value = cursor.getString(columnIndex1);
                                                Log.v(TAG,"value= "+value);
                                                break;
                                            }
                                        } while (cursor.moveToNext());
                                        //socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        //        Integer.parseInt(origin));
                                        try {
                                            // Log.v(TAG, "Sending the reply now==" + fhmp);
                                            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
                                            os.writeObject(value);
                                            os.flush();
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                        try {
                            if (hm1.get("requesttype").equals("insert")) {
                                Log.v(TAG, "insert request received = " + hm1);
                                ContentValues values = new ContentValues();
                                values.put("key", hm1.get("key"));
                                values.put("value", hm1.get("msg"));
                                values.put("hash", hm1.get("msgHash"));
                                values.put("port", hm1.get("port"));
                                values.put("requesttype","selfinsert");
                                insert(mUri, values);
                               // break;
                            }

                        }catch(Exception e){
                            e.printStackTrace();
                        }
                        try {//BQNIWFvQfvbc3GIratUf0OE0zd5mTd09
                            if (hm1.get("requesttype").equals("remap")) {

                                Log.v(TAG, "I got the remapping request");
                                Log.v(TAG, "===================================================================================");
                                Log.v(TAG, "final seq=" + hm1);

                                for (HashMap.Entry<String,String> entry : hm1.entrySet())
                                {
                                    if(!entry.getKey().equals("requesttype")) {
                                        seq.add(entry.getKey());
                                        currNodes.put(entry.getKey(), entry.getValue());
                                    }
                                }

                                Collections.sort(seq);
                                joining = "yes";

                                for (String i : seq) {
                                    avdseq.add(currNodes.get(i));
                                }
                            }

                        }catch(Exception e){
                            e.printStackTrace();
                        }

                    try {
                        if (hm1.get("requesttype").equals("join")) {

                            String i = hm1.get("avd");
                            Log.v(TAG,"Receiving the Joining requests from avd "+i);

                            /*
                             for (Map.Entry<String,String> entry : gfg.entrySet())
            System.out.println("Key = " + entry.getKey() +
                             ", Value = " + entry.getValue());
                            */

                            Nodes.put(hm1.get("hash"),i);
                            //pq.add(hm1);
                            i1 = i1 + 1;

                           // if (i1 > 4) {

                              /*  Iterator value1 = pq.iterator();
                                Log.v(TAG, "priority queue values in priority =");
                                int i2 = 0;
                                while (value1.hasNext()) {
                                    //System.out.println(pq.peek());
                                    HashMap<String, String> hm5 = new HashMap<String, String>();
                                    hm5 = pq.poll();
                                    //Log.v(TAG,"in priority="+hm5);
                                    ringOfAvds[i2] = Integer.valueOf(hm5.get("avd"));
                                    hashOfAvds[i2] = hm5.get("hash");
                                    fhmp.put("avd" + i2, hm5.get("avd"));
                                    fhmp.put("avdhash" + hm5.get("avd"), hm5.get("hash"));
                                    i2++;
                                }
                              */  // }

                                //if (i1 > 4) {
                                for (int i4 = 0; i4 < i1; i4++) {
                                    try {
                                        socketarray1[i4] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                                Integer.parseInt(remote_ports[i4]));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                //fhmp.put("requesttype", "remap");
                                Nodes.put("requesttype", "remap");
                                //fhmp.put("seq", "final");
                                for (int j = 0; j < i1; j++) {
                                    try {
                                        // Log.v(TAG, "Sending the reply now==" + fhmp);
                                        ObjectOutputStream os = new ObjectOutputStream(socketarray1[j].getOutputStream());
                                        os.writeObject(Nodes);
                                        os.flush();
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }

                                }
                            }
                        //}
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    } catch(Exception e){
                        e.printStackTrace();
                    }
        }
           // return null;
        }
}


   private class ClientTask extends AsyncTask<String, Void, Void> {

        String msgToSend;


        @SuppressLint("LongLogTag")
        @Override
        protected Void doInBackground(String... msgs) {


            Log.v(TAG, "Inside Client");

            if(msgs[0].equals("query*")){
                Log.v(TAG,msgs[0]);

                int count =0;
                for(String a:seq){
                    count++;
                }
                Log.v(TAG,"curretn nodes = "+count);
                MatrixCursor cursortmp1 = new MatrixCursor(new String[]{"key", "value"});
                int i=0;
                for(i=0;i<count;i++){

                    try {
                        //for (int i = 0; i < 5; i++) {
                        socketarray2[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remote_ports[i]));
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }

                    Log.v(TAG,"sending the request for quer *");
                    HashMap<String,String> hmquery = new HashMap<String, String>();
                    hmquery.put("requesttype",msgs[0]);
                    hmquery.put("origin",myportclient);
                    try {
                        ObjectOutputStream os;
                        try {
                            os = new ObjectOutputStream(socketarray2[i].getOutputStream());
                            os.writeObject(hmquery);
                            os.flush();
                        }catch (Exception ex){
                            ex.printStackTrace();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    //receiving the propose sequence no. as a response from all avd including from himself
                    Log.v(TAG,"received the reply for query *");
                    HashMap<String,String> tmp = new HashMap<String, String>();
                    ObjectInputStream is = null;
                    try {
                        is = new ObjectInputStream(socketarray2[i].getInputStream());
                        try {

                            tmp= (HashMap<String, String>) is.readObject();

                            //is.close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        for(HashMap.Entry<String,String> entry : tmp.entrySet()) {
                            querstarreply.put(entry.getKey(),entry.getValue());
                            Log.v(TAG,"querstarreply="+querstarreply);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.v(TAG,i+"~~~"+count);
                    }
                if(i==count){
                    Log.v(TAG,"ans=yes");
                    ans="yes";

                }
            }
            else if(msgs[0].equals("insert")) {
                Log.v(TAG,msgs[0]+" "+msgs[1]+" "+msgs[2]+" "+msgs[3]+" "+msgs[4]);
                Socket socket = null;
                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[4]));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //String mssg1 = (String) values.get("value");
                HashMap<String, String> hminsert = new HashMap<String, String>();
                hminsert.put("requesttype", msgs[0]);
                hminsert.put("msg", msgs[1]);
                hminsert.put("key", msgs[2]);
                hminsert.put("msgHash", msgs[3]);
                hminsert.put("port", msgs[4]);
                //hminsert.put("remap","-");
                try {
                    ObjectOutputStream os;
                    try {
                        //Log.v(TAG," inside outputstream block");
                        Log.v(TAG, "sending a insert  request =" + hminsert);
                        os = new ObjectOutputStream(socket.getOutputStream());
                        os.writeObject(hminsert);
                        os.flush();
                        //os.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    //  socket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }else if(msgs[0].equals("query")) {
                    Log.v(TAG,msgs[0]+" hash = "+msgs[1]+" port = "+msgs[2]+" key ="+msgs[3]);
                    Socket socket = null;
                    try {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[2]));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //String mssg1 = (String) values.get("value");
                    HashMap<String, String> hmquery = new HashMap<String, String>();
                    hmquery.put("requesttype", msgs[0]);
                    hmquery.put("key", msgs[3]);
                    hmquery.put("msgHash", msgs[1]);
                    hmquery.put("port", msgs[2]);
                    hmquery.put("origin",String.valueOf(myportclient));

                //requesttype,msgHash1,myPort1,key

                    //hminsert.put("remap","-");
                    try {
                        ObjectOutputStream os;
                        try {
                            //Log.v(TAG," inside outputstream block");
                            Log.v(TAG, "sending a query  request =" + hmquery);
                            os = new ObjectOutputStream(socket.getOutputStream());
                            os.writeObject(hmquery);
                            os.flush();
                            //os.close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        //  socket.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    try {
                        ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
                        queryvalue = (String) is.readObject();

                    }catch(Exception e){
                        e.printStackTrace();
                    }
            }else{
                Log.v(TAG, " msgs =" + msgs[0] + " " + msgs[1]);

                Socket socket = null;
                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), 11108);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Integer i = portsmap.get(msgs[1]);
                //mssg.put("msgtosend", msgs[0]);
                mssg.put("remoteport", String.valueOf(msgs[1]));
                mssg.put("hash", String.valueOf(msgs[0]));
                mssg.put("avd", String.valueOf(i));
                //mssg.put("remap","none");
                mssg.put("requesttype", "join");
                try {
                    ObjectOutputStream os;
                    try {
                        //Log.v(TAG," inside outputstream block");
                        Log.v(TAG, "sending a join request=" + mssg);
                        os = new ObjectOutputStream(socket.getOutputStream());
                        os.writeObject(mssg);
                        os.flush();
                        //os.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    //  socket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            // Log.v(TAG, "return msg=" + hm2);
            return null;


        }
    }
}
//Final Submission Code