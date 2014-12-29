package org.kde.kdeconnect.Plugins.MprisPlugin;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.kde.kdeconnect.Device;

/**
 * Created by Da-Jin on 12/29/2014.
 */
@SuppressLint("NewApi")
public class RemoteControlClientManager {


    private final String deviceId;
    private final String player;
    private final Context context;
    private ComponentName eventReceiver;
    private RemoteControlClient remoteControlClient;
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener focusListener;
    private String song;

    public RemoteControlClientManager(Context context, Device device, String player){
        this.context = context;
        this.deviceId = device.getDeviceId();
        this.player = player;

        audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

        focusListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {

            }
        };

        //TODO: NotificationPanel and RemoteControlClientManager are nearly the same, they are separated for clarity put them back together when a good name is found
        final MprisPlugin mpris = (MprisPlugin)device.getPlugin("plugin_mpris");
        if (mpris != null) {
            mpris.setPlayerStatusUpdatedHandler("remoteclient", new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    String song = mpris.getCurrentSong();
                    boolean isPlaying = mpris.isPlaying();
                    updateStatus(song, isPlaying);
                }
            });
        }
    }
    private void registerRemoteClient(){
        eventReceiver = new ComponentName(context.getPackageName(), NotificationReturnSlot.class.getName());
        if(remoteControlClient==null) {
            audioManager.registerMediaButtonEventReceiver(eventReceiver);
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            Log.d("Panel", deviceId + " " + player);
            mediaButtonIntent.putExtra("ID", deviceId);
            mediaButtonIntent.putExtra("PLAYER", player);
            Log.d("Panel", mediaButtonIntent.getStringExtra("ID") + " " + mediaButtonIntent.getStringExtra("PLAYER"));
            mediaButtonIntent.setComponent(eventReceiver);
            PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context, 0, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteControlClient = new RemoteControlClient(mediaPendingIntent);
            audioManager.registerRemoteControlClient(remoteControlClient);
        }
        remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                        | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
                        | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                        | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                        | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
        );
    }
    private void unregisterRemoteClient(){
        audioManager.unregisterMediaButtonEventReceiver(eventReceiver);
        audioManager.unregisterRemoteControlClient(remoteControlClient);
        remoteControlClient=null;
    }
    private void updateMetadata(){
        if(remoteControlClient==null){
            return;
        }
        RemoteControlClient.MetadataEditor metatdataEditor = remoteControlClient.editMetadata(true);
        metatdataEditor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, song);
        metatdataEditor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, player);
        metatdataEditor.apply();
        Log.d("RemoteClient",song+" "+player);
    };
    public void updateStatus(String songName, boolean isPlaying){
        song = songName;
        if(isPlaying){
            audioManager.requestAudioFocus(focusListener,AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            registerRemoteClient();
            if(remoteControlClient!=null)
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            updateMetadata();
        }else{
            audioManager.abandonAudioFocus(focusListener);
            if(remoteControlClient!=null)
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        }
    }
}
