package com.ivianuu.conductor.changehandler

import android.annotation.TargetApi
import android.os.Build
import android.transition.Transition
import android.transition.Transition.TransitionListener
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup

import com.ivianuu.conductor.Controller
import com.ivianuu.conductor.ControllerChangeHandler

/**
 * A base [ControllerChangeHandler] that facilitates using [android.transition.Transition]s to replace Controller Views.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
abstract class TransitionChangeHandler : ControllerChangeHandler() {

    internal var canceled = false
    private var needsImmediateCompletion = false

    interface OnTransitionPreparedListener {
        fun onPrepared()
    }

    /**
     * Should be overridden to return the Transition to use while replacing Views.
     *
     * @param container The container these Views are hosted in
     * @param from      The previous View in the container or `null` if there was no Controller before this transition
     * @param to        The next View that should be put in the container or `null` if no Controller is being transitioned to
     * @param isPush    True if this is a push transaction, false if it's a pop
     */
    protected abstract fun getTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean
    ): Transition

    override fun onAbortPush(newHandler: ControllerChangeHandler, newTop: Controller?) {
        super.onAbortPush(newHandler, newTop)

        canceled = true
    }

    override fun completeImmediately() {
        super.completeImmediately()

        needsImmediateCompletion = true
    }

    override fun performChange(
        container: ViewGroup,
        from: View?,
        to: View?,
        isPush: Boolean,
        changeListener: ControllerChangeHandler.ControllerChangeCompletedListener
    ) {
        if (canceled) {
            changeListener.onChangeCompleted()
            return
        }
        if (needsImmediateCompletion) {
            executePropertyChanges(container, from, to, null, isPush)
            changeListener.onChangeCompleted()
            return
        }

        val transition = getTransition(container, from, to, isPush)
        transition.addListener(object : TransitionListener {
            override fun onTransitionStart(transition: Transition) {}

            override fun onTransitionEnd(transition: Transition) {
                changeListener.onChangeCompleted()
            }

            override fun onTransitionCancel(transition: Transition) {
                changeListener.onChangeCompleted()
            }

            override fun onTransitionPause(transition: Transition) {}

            override fun onTransitionResume(transition: Transition) {}
        })

        prepareForTransition(
            container,
            from,
            to,
            transition,
            isPush,
            object : OnTransitionPreparedListener {
                override fun onPrepared() {
                    if (!canceled) {
                        TransitionManager.beginDelayedTransition(container, transition)
                        executePropertyChanges(container, from, to, transition, isPush)
                    }
                }
            })
    }

    override fun removesFromViewOnPush(): Boolean {
        return true
    }

    /**
     * Called before a transition occurs. This can be used to reorder views, set their transition names, etc. The transition will begin
     * when `onTransitionPreparedListener` is called.
     *
     * @param container  The container these Views are hosted in
     * @param from       The previous View in the container or `null` if there was no Controller before this transition
     * @param to         The next View that should be put in the container or `null` if no Controller is being transitioned to
     * @param transition The transition that is being prepared for
     * @param isPush     True if this is a push transaction, false if it's a pop
     */
    open fun prepareForTransition(
        container: ViewGroup,
        from: View?,
        to: View?,
        transition: Transition,
        isPush: Boolean,
        onTransitionPreparedListener: OnTransitionPreparedListener
    ) {
        onTransitionPreparedListener.onPrepared()
    }

    /**
     * This should set all view properties needed for the transition to work properly. By default it removes the "from" view
     * and adds the "to" view.
     *
     * @param container  The container these Views are hosted in
     * @param from       The previous View in the container or `null` if there was no Controller before this transition
     * @param to         The next View that should be put in the container or `null` if no Controller is being transitioned to
     * @param transition The transition with which `TransitionManager.beginDelayedTransition` has been called. This will be null only if another ControllerChangeHandler immediately overrides this one.
     * @param isPush     True if this is a push transaction, false if it's a pop
     */
    open fun executePropertyChanges(
        container: ViewGroup,
        from: View?,
        to: View?,
        transition: Transition?,
        isPush: Boolean
    ) {
        if (from != null && (removesFromViewOnPush() || !isPush) && from.parent == container) {
            container.removeView(from)
        }
        if (to != null && to.parent == null) {
            container.addView(to)
        }
    }

}
