package com.example.bounceball.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import com.example.bounceball.R;

public class SoundManager {
    private SoundPool soundPool;
    private GamePreferences prefs;

    public static final int SOUND_BOUNCE = 1;
    public static final int SOUND_FALL = 2;
    public static final int SOUND_WARP_IN = 3;
    public static final int SOUND_WARP_OUT = 4;
    public static final int SOUND_INK = 5;

    private int sBounce, sFall, sWarpIn, sWarpOut, sInk;
    private int sElemFire, sElemWater, sElemEarth, sElemIce, sElemDark, sElemLight, sElemAir, sElemLightning, sElemPlasma, sElemLava;

    private int currentElemStream = -1;

    public SoundManager(Context context, GamePreferences prefs) {
        this.prefs = prefs;

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        sBounce = soundPool.load(context, R.raw.bounce, 1);
        sFall = soundPool.load(context, R.raw.fall, 1);
        sWarpIn = soundPool.load(context, R.raw.warp_in, 1);
        sWarpOut = soundPool.load(context, R.raw.warp_out, 1);
        sInk = soundPool.load(context, R.raw.ink_pickup, 1);

        sElemFire = soundPool.load(context, R.raw.elem_fire, 1);
        sElemWater = soundPool.load(context, R.raw.elem_water, 1);
        sElemEarth = soundPool.load(context, R.raw.elem_earth, 1);
        sElemIce = soundPool.load(context, R.raw.elem_ice, 1);
        sElemDark = soundPool.load(context, R.raw.elem_dark, 1);
        sElemLight = soundPool.load(context, R.raw.elem_light, 1);
        sElemAir = soundPool.load(context, R.raw.elem_air, 1);
        sElemLightning = soundPool.load(context, R.raw.elem_lightning, 1);
        sElemPlasma = soundPool.load(context, R.raw.elem_plasma, 1);
        sElemLava = soundPool.load(context, R.raw.elem_lava, 1);
    }

    public int playSound(int soundId) {
        if (soundPool == null || !prefs.isSoundEnabled()) return -1;

        int idToPlay = -1;
        switch(soundId) {
            case SOUND_BOUNCE: idToPlay = sBounce; break;
            case SOUND_FALL: idToPlay = sFall; break;
            case SOUND_WARP_IN: idToPlay = sWarpIn; break;
            case SOUND_WARP_OUT: idToPlay = sWarpOut; break;
            case SOUND_INK: idToPlay = sInk; break;
        }
        if (idToPlay != -1 && idToPlay != 0) {
            return soundPool.play(idToPlay, 1.0f, 1.0f, 0, 0, 1.0f);
        }
        return -1;
    }

    public void playElementalSound(String skinId) {
        if (soundPool == null || !prefs.isSoundEnabled()) return;

        if (currentElemStream != -1) {
            soundPool.stop(currentElemStream);
        }

        int idToPlay = -1;
        switch(skinId) {
            case "ball_elem_fire": idToPlay = sElemFire; break;
            case "ball_elem_water": idToPlay = sElemWater; break;
            case "ball_elem_earth": idToPlay = sElemEarth; break;
            case "ball_elem_ice": idToPlay = sElemIce; break;
            case "ball_elem_darkness": idToPlay = sElemDark; break;
            case "ball_elem_light": idToPlay = sElemLight; break;
            case "ball_elem_air": idToPlay = sElemAir; break;
            case "ball_elem_lightning": idToPlay = sElemLightning; break;
            case "ball_elem_plasma": idToPlay = sElemPlasma; break;
            case "ball_elem_lava": idToPlay = sElemLava; break;
        }
        if (idToPlay != -1 && idToPlay != 0) {
            currentElemStream = soundPool.play(idToPlay, 1.0f, 1.0f, 0, 0, 1.0f);
        } else {
            currentElemStream = playSound(SOUND_BOUNCE);
        }
    }

    public void stopElementalSound() {
        if (currentElemStream != -1 && soundPool != null) {
            soundPool.stop(currentElemStream);
            currentElemStream = -1;
        }
    }

    public void stopSound(int streamId) {
        if (streamId != -1 && soundPool != null) {
            soundPool.stop(streamId);
        }
    }

    public void setVolume(int streamId, float volume) {
        if (streamId != -1 && soundPool != null) {
            soundPool.setVolume(streamId, volume, volume);
        }
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}