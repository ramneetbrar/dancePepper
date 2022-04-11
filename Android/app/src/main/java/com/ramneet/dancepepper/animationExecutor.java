package com.ramneet.dancepepper;

import android.util.Log;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.conversation.BaseQiChatExecutor;

import java.lang.reflect.Field;
import java.util.List;

public class animationExecutor extends BaseQiChatExecutor {
    private final QiContext qiContext;
    private String animationType;

    protected animationExecutor(QiContext context, String animationType) {
        super(context);
        this.qiContext = context;
        this.animationType = animationType;
    }

    @Override
    public void runWith(List<String> params) {
        if (animationType.equalsIgnoreCase("animate")){
            animationType = "boogie"; // replace with motion name from camera
        }
        animate(qiContext, animationType);
    }

    public void run(String prediction) {
        if (animationType.equalsIgnoreCase("animate")){
            animationType = prediction; // replace with motion name from camera
        }
        animate(qiContext, animationType);
    }

    @Override
    public void stop() {
        Log.i("AnimationExecutor", "QiChatExecutor stopped");
    }

    private void animate(QiContext qiContext, String motion) {
        try {
//            Class res = R.raw.class;
//            Field field = res.getDeclaredField(motion);
//            int drawableId = field.getInt(null);

            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try  {
                        //Your code goes here
                        Class res = R.raw.class;
                        Field field = res.getDeclaredField(motion);
                        int drawableId = field.getInt(null);
                        // Create an animation.
                        Animation animation = AnimationBuilder.with(qiContext)
                                .withResources(drawableId)
                                .build();

                        // Create an animate action.
                        Animate animate = AnimateBuilder.with(qiContext)
                                .withAnimation(animation)
                                .build();
                        animate.run();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();
        }
        catch (Exception e) {
            Log.e("animationExecutor", "Failure to get drawable id.", e);
        }
    }
}
