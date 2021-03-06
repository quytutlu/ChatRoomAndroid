package com.example.androidclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import com.sinch.android.rtc.messaging.WritableMessage;

public class MainActivity extends Activity {
	
	EditText NoiDung;
	Socket sk;
	boolean DongSocket=false;
	String TenDangNhap;
	boolean HienThongBao=true;
	private MessageAdapter messageAdapter;
    private ListView messagesList;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.messaging);
		Intent intent = getIntent();
		TenDangNhap = intent.getStringExtra("TenDangNhap");
		NoiDung=(EditText) findViewById(R.id.NoiDungTinNhan);
		messagesList = (ListView) findViewById(R.id.listTinNhan);
        messageAdapter = new MessageAdapter(this);
        messagesList.setAdapter(messageAdapter);
        messagesList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			}
        	
		});
		MyClientTask myClientTask = new MyClientTask("52.68.172.187",2015);
        //MyClientTask myClientTask = new MyClientTask("192.168.1.104",2015);
		myClientTask.execute();
	}
	@Override
	protected void onPause() {
		HienThongBao=true;
		super.onPause();
	}
	@Override
	protected void onResume() {
		HienThongBao=false;
		cancelNotification(0);
		super.onResume();
	}
	@Override
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
	    return false;
	}
	public void NgatKetNoi(){
		DataOutputStream ps;
        try {
            ps = new DataOutputStream(sk.getOutputStream());
            ps.writeUTF(TenDangNhap+"!@#.exit\r\n");
        } catch (IOException ex) {
        	ex.printStackTrace();
        }
        try {
			sk.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        DongSocket=true;
	}
	@Override
	public void onBackPressed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle("Xác nhận");
		builder.setMessage("Bạn có chắc chắc muốn thoát không?");
		builder.setNegativeButton("Có", new OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				NgatKetNoi();
				Intent intent = new Intent(MainActivity.this,Login.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
			}
		});
		builder.setPositiveButton("Không", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();

			}
		});
		builder.show();
		
	}
	public void GuiTinNhan(View v){
		String data=NoiDung.getText().toString();
		data=data.trim();
		if(!data.equals("")){
			DataOutputStream ps;
			try {
				ps = new DataOutputStream(sk.getOutputStream());
				ps.writeUTF(TenDangNhap + "!@#." + NoiDung.getText() + "\r\n");
				NoiDung.setText("");

			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	public class MyClientTask extends AsyncTask<Void, Void, Void> {
		
		String dstAddress;
		int dstPort;
		
		MyClientTask(String addr, int port){
			dstAddress = addr;
			dstPort = port;
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			
			Socket socket = null;
			
			try {
				socket = new Socket(dstAddress, dstPort);
				sk=socket;
				DataOutputStream ps;
		        try {
		            ps = new DataOutputStream(socket.getOutputStream());
		            ps.writeUTF("online!@#."+TenDangNhap+"\r\n");

		        } catch (IOException ex) {
		        	ex.printStackTrace();
		        }
				while (!DongSocket) {
                    DataInputStream br;
                    try {
                        br = new DataInputStream(socket.getInputStream());
                        String str = br.readUTF();
                        str = str.substring(0, str.indexOf("\r\n"));
                        final String []st=str.split(":");
                        System.out.println(str);
                        final String temp=str;
                        runOnUiThread(new Runnable() {
							@Override
							public void run() {
								String TinNhan=st[0]+"!@#.>"+temp.subSequence(st[0].length()+1, temp.length());
								if(st[0].equals("BẠN")){
									messageAdapter.addMessage(new WritableMessage(TinNhan, TinNhan), MessageAdapter.DIRECTION_INCOMING);
								}else{
									messageAdapter.addMessage(new WritableMessage(TinNhan,TinNhan), MessageAdapter.DIRECTION_OUTGOING);
								}
								
								if(HienThongBao && !st[0].equals("BẠN")){
									if(st[1].length()>30){
										showNotification("Bạn có tin nhắn từ: "+st[0],st[1].substring(0, 30)+"...");
									}else{
										showNotification("Bạn có tin nhắn từ: "+st[0],st[1]);
									}
									
								}
							}
						});
                    } catch (IOException ex) {
                        
                    }
                }

			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	public void showNotification(String Tile,String Content){

		// define sound URI, the sound to be played when there's a notification
		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		// intent triggered, you can add other intent for other actions
		Intent intent = new Intent(MainActivity.this, MainActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);
		
		// this is it, we'll build the notification!
		// in the addAction method, if you don't want any icon, just set the first param to 0
		Notification mNotification = new Notification.Builder(this)
			
			.setContentTitle(Tile)
			.setContentText(Content)
			.setSmallIcon(R.drawable.logo2)
			.setContentIntent(pIntent)
			.setSound(soundUri)
			
			//.addAction(0, "Xem", pIntent)
			//.addAction(0, "Remind", pIntent)
			
			.build();
		
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// If you want to hide the notification after it was selected, do the code below
		// myNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		notificationManager.notify(0, mNotification);
	}
	
	public void cancelNotification(int notificationId){
		
		if (Context.NOTIFICATION_SERVICE!=null) {
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(ns);
            nMgr.cancel(notificationId);
        }
	}
}
