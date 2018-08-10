package de.symeda.sormas.app.report;

import android.content.Context;
import android.databinding.ViewDataBinding;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.android.databinding.library.baseAdapters.BR;
import com.google.android.gms.analytics.Tracker;

import de.symeda.sormas.app.R;
import de.symeda.sormas.app.SormasApplication;
import de.symeda.sormas.app.component.controls.ControlButtonType;
import de.symeda.sormas.app.component.dialog.BaseTeboAlertDialog;
import de.symeda.sormas.app.core.Callback;
import de.symeda.sormas.app.databinding.DialogMissingWeeklyReportLayoutBinding;

public class MissingWeeklyReportDialog extends BaseTeboAlertDialog {

    public static final String TAG = MissingWeeklyReportDialog.class.getSimpleName();

    public MissingWeeklyReportDialog(final FragmentActivity activity) {
        this(activity, R.string.heading_missing_weekly_report_dialog, R.string.alert_missing_report);
    }

    public MissingWeeklyReportDialog(final FragmentActivity activity, int headingResId, int subHeadingResId) {
        super(activity, R.layout.dialog_root_layout, R.layout.dialog_missing_weekly_report_layout,
                R.layout.dialog_root_two_button_panel_edge_aligned_layout, headingResId, subHeadingResId);
    }

    @Override
    protected void onOkClicked(View v, Object item, View rootView, ViewDataBinding contentBinding, Callback.IAction callback) {
        if (callback != null)
            callback.call(null);
    }

    @Override
    protected void onDismissClicked(View v, Object item, View rootView, ViewDataBinding contentBinding, Callback.IAction callback) {
        if (callback != null)
            callback.call(null);
    }

    @Override
    protected void onDeleteClicked(View v, Object item, View rootView, ViewDataBinding contentBinding, Callback.IAction callback) {
        if (callback != null)
            callback.call(null);
    }

    @Override
    protected void recieveViewDataBinding(Context context, ViewDataBinding binding) {

    }

    @Override
    protected void setBindingVariable(Context context, ViewDataBinding binding, String layoutName) {

    }

    @Override
    protected void prepareDialogData() {

    }

    @Override
    protected void initializeContentView(ViewDataBinding rootBinding, ViewDataBinding contentBinding, ViewDataBinding buttonPanelBinding) {

    }

    @Override
    public boolean isOkButtonVisible() {
        return true;
    }

    @Override
    public boolean isHeadingCentered() {
        return true;
    }

    @Override
    public boolean isRounded() {
        return true;
    }

    @Override
    public float getWidth() {
        return getContext().getResources().getDimension(R.dimen.notificationDialogWidth);
    }

    @Override
    public ControlButtonType dismissButtonType() {
        return ControlButtonType.LINE_DANGER;
    }

    @Override
    public int getPositiveButtonText() {
        return R.string.action_open_reports;
    }

    @Override
    public boolean iconOnlyDismissButtons() {
        return true;
    }
}
