package com.example.dongja94.samplemediaplay;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    MediaPlayer mPlayer;

    enum PlayState {
        IDLE,
        INITIALIZED,
        PREPARED,
        STARTED,
        PAUSED,
        STOPPED,
        ERROR,
        RELEASED
    }

    PlayState mState = PlayState.IDLE;
    SeekBar progressView, volumeView;
    AudioManager mAM;
    CheckBox muteView;

    float volume = 1.0f;

    Runnable volumeUp = new Runnable() {
        @Override
        public void run() {
            if (volume < 1.0f) {
                mPlayer.setVolume(volume, volume);
                volume+=0.1f;
                mHandler.postDelayed(this, CHECK_INTERVAL);
            } else {
                volume = 1.0f;
                mPlayer.setVolume(volume,volume);
            }
        }
    };

    Runnable volumeDown = new Runnable() {
        @Override
        public void run() {
            if (volume > 0) {
                mPlayer.setVolume(volume, volume);
                volume -= 0.1f;
                mHandler.postDelayed(this, CHECK_INTERVAL);
            } else {
                volume = 0;
                mPlayer.setVolume(volume,volume);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        muteView = (CheckBox)findViewById(R.id.check_mute);

        muteView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mHandler.removeCallbacks(volumeUp);
                    mHandler.post(volumeDown);
                } else {
                    mHandler.removeCallbacks(volumeDown);
                    mHandler.post(volumeUp);
                }
            }
        });
        mAM = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        volumeView = (SeekBar)findViewById(R.id.seek_volume);
        int max = mAM.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volumeView.setMax(max);
        int current = mAM.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeView.setProgress(current);

        volumeView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mAM.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mPlayer = MediaPlayer.create(this, R.raw.winter_blues);
        mState = PlayState.PREPARED;

        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mState = PlayState.ERROR;
                return false;
            }
        });

        Button btn = (Button)findViewById(R.id.btn_play);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        btn = (Button)findViewById(R.id.btn_pause);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause();
            }
        });

        btn = (Button)findViewById(R.id.btn_stop);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });

        btn = (Button)findViewById(R.id.btn_list);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MusicListActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        progressView = (SeekBar)findViewById(R.id.seek_progress);

        progressView.setMax(mPlayer.getDuration());

        progressView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    this.progress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progress = -1;
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (progress != -1) {
                    if (mState == PlayState.STARTED) {
                        mPlayer.seekTo(progress);
                    }
                }
                isSeeking = false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            String name = data.getStringExtra("name");
            Uri uri = data.getData();
            setTitle(name);
            mPlayer.reset();
            mState = PlayState.IDLE;

            try {
                mPlayer.setDataSource(this, uri);
                mState = PlayState.INITIALIZED;

                mPlayer.prepare();
                mState = PlayState.PREPARED;

                int max = mPlayer.getDuration();
                progressView.setMax(max);

                progressView.setProgress(0);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    boolean isSeeking = false;

    Handler mHandler = new Handler(Looper.getMainLooper());

    private static final int CHECK_INTERVAL = 100;

    Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mState == PlayState.STARTED) {
                if (!isSeeking) {
                    int position = mPlayer.getCurrentPosition();
                    progressView.setProgress(position);
                }
                mHandler.postDelayed(this, CHECK_INTERVAL);
            }
        }
    };

    private void play() {
        if (mState == PlayState.INITIALIZED || mState == PlayState.STOPPED) {
            try {
                mPlayer.prepare();
                mState = PlayState.PREPARED;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mState == PlayState.PREPARED || mState == PlayState.PAUSED) {
            mPlayer.seekTo(progressView.getProgress());
            mPlayer.start();
            mState = PlayState.STARTED;
            mHandler.post(progressRunnable);
        }
    }

    private void pause() {
        if (mState == PlayState.STARTED) {
            mPlayer.pause();
            mState = PlayState.PAUSED;
        }
    }

    private void stop() {
        if (mState == PlayState.STARTED || mState == PlayState.PREPARED || mState == PlayState.PAUSED) {
            mPlayer.stop();
            mState = PlayState.STOPPED;
            progressView.setProgress(0);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayer != null) {
            mPlayer.release();
            mState = PlayState.RELEASED;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
