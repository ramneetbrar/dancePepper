package com.ramneet.dancepepper;

import android.util.Log;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.AnimateBuilder;
import com.aldebaran.qi.sdk.builder.AnimationBuilder;
import com.aldebaran.qi.sdk.object.actuation.Animate;
import com.aldebaran.qi.sdk.object.actuation.Animation;
import com.aldebaran.qi.sdk.object.conversation.BaseQiChatExecutor;

import java.util.List;

public class animationExecutor extends BaseQiChatExecutor {
    private final QiContext qiContext;

    protected animationExecutor(QiContext context) {
        super(context);
        this.qiContext = context;
    }

    @Override
    public void runWith(List<String> params) {
        animate(qiContext);
    }

    @Override
    public void stop() {
        Log.i("AnimationExecutor", "QiChatExecutor stopped");
    }

    private void animate(QiContext qiContext) {
        // Create an animation.
        Animation animation = AnimationBuilder.with(qiContext) // Create the builder with the context.
                .withResources(R.raw.clapping_b001) // Set the animation resource.
                .build(); // Build the animation.

        // Create an animate action.
        Animate animate = AnimateBuilder.with(qiContext) // Create the builder with the context.
                .withAnimation(animation) // Set the animation.
                .build(); // Build the animate action.
        animate.run();
    }
}
