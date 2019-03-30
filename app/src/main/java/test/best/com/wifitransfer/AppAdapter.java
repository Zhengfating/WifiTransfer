package test.best.com.wifitransfer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hwangjr.rxbus.RxBus;

import java.io.File;
import java.util.List;

import static test.best.com.wifitransfer.AppInstallOrDelete.delete;
import static test.best.com.wifitransfer.AppInstallOrDelete.installApkFile;

public class AppAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<InfoModel> myApps;
    private boolean installAllowed = false;
    private Context context;

    public AppAdapter(List<InfoModel> myApps, Context context) {
        this.myApps = myApps;
        this.context = context;
    }

    static class ViewHolder extends RecyclerView.ViewHolder{

        TextView mTvAppName;
        TextView mTvAppSize;
        TextView mTvAppInstall;
        TextView mTvAppDelete;
        TextView mTvAppPath;
        ImageView ivIcon;

        public ViewHolder(View itemView) {
            super(itemView);

            mTvAppName = itemView.findViewById(R.id.tv_name);
            mTvAppSize = itemView.findViewById(R.id.tv_size);
            mTvAppInstall = itemView.findViewById(R.id.tv_install);
            mTvAppPath = itemView.findViewById(R.id.tv_path);
            mTvAppDelete = itemView.findViewById(R.id.tv_delete);
            ivIcon = itemView.findViewById(R.id.iv_icon);
        }
    }

    static class EmptyViewHolder extends RecyclerView.ViewHolder {

        public EmptyViewHolder(View itemView) {
            super(itemView);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 1){
            View view = inflater.inflate(R.layout.empty_view, parent, false);
            holder = new EmptyViewHolder(view);
        }else {
            holder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_apk_item, parent, false));
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder1, int position) {
        if (holder1 instanceof ViewHolder){
            ViewHolder holder = (ViewHolder) holder1;
            final InfoModel infoModel = myApps.get(position);
            holder.mTvAppName.setText(infoModel.getName() + "(v" + infoModel.getVersion() + ")");
            holder.mTvAppSize.setText(infoModel.getSize());
            holder.mTvAppPath.setText(infoModel.getPath());
            holder.ivIcon.setImageDrawable(infoModel.getIcon());
            holder.mTvAppInstall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        installAllowed = context.getPackageManager().canRequestPackageInstalls();
                        if (installAllowed) {
                            installApkFile(new File(infoModel.getPath()), context);
                        } else {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                            installApkFile(new File(infoModel.getPath()), context);
                            return;
                        }
                    } else {
                        installApkFile(new File(infoModel.getPath()), context);
                    }
                }
            });
            holder.mTvAppDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    delete(infoModel.getPackageName(), context);

                }
            });

            if (infoModel.isInstalled()){
                holder.mTvAppDelete.setVisibility(View.VISIBLE);
            }else {
                holder.mTvAppDelete.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return myApps.size() > 0 ? myApps.size() : 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (myApps.size() == 0) {
            return 1;
        }
        return super.getItemViewType(position);
    }


}
