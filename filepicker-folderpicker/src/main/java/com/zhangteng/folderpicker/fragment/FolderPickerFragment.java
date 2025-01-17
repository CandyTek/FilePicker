package com.zhangteng.folderpicker.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zhangteng.androidpermission.AndroidPermission;
import com.zhangteng.androidpermission.Permission;
import com.zhangteng.androidpermission.callback.Callback;
import com.zhangteng.common.callback.IHandlerCallBack;
import com.zhangteng.common.config.FilePickerConfig;
import com.zhangteng.folderpicker.R;
import com.zhangteng.folderpicker.adapter.FolderPickerAdapter;
import com.zhangteng.searchfilelibrary.FileService;
import com.zhangteng.searchfilelibrary.config.SearchCofig;
import com.zhangteng.searchfilelibrary.entity.MediaEntity;
import com.zhangteng.searchfilelibrary.utils.MediaStoreUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件选择器
 */
public class FolderPickerFragment extends Fragment {
    private RecyclerView mRecyclerViewImageList;
    private TextView mTextViewPreview;
    private TextView mTextViewSelected;
    private TextView mTextViewUpload;
    private Context mContext;

    private ArrayList<MediaEntity> imageInfos;

    private File currentFile = new File(SearchCofig.BASE_SD_PATH);

    private FolderPickerAdapter folderPickerAdapter;
    private FilePickerConfig folderPickerConfig;
    private IHandlerCallBack iHandlerCallBack;
    private List<MediaEntity> selectFolder;

    public FolderPickerFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_folder_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        initData();
    }

    protected void initView(View view) {
        mRecyclerViewImageList = view.findViewById(R.id.folder_picker_rv_list);
        mRecyclerViewImageList.setLayoutManager(new LinearLayoutManager(getContext()));
        mTextViewPreview = view.findViewById(R.id.file_picker_tv_preview);
        mTextViewSelected = view.findViewById(R.id.file_picker_tv_selected);
        mTextViewUpload = view.findViewById(R.id.file_picker_tv_upload);
        mTextViewPreview.setOnClickListener(v -> iHandlerCallBack.onPreview(selectFolder));
        mTextViewSelected.setOnClickListener(view1 -> iHandlerCallBack.onSuccess(selectFolder));
        mTextViewUpload.setOnClickListener(view12 -> {
            iHandlerCallBack.onSuccess(selectFolder);
            iHandlerCallBack.onFinish(selectFolder);
            if (null != getActivity()) {
                getActivity().finish();
            }
        });
    }

    public void initData() {
        folderPickerConfig = FilePickerConfig.getInstance();
        selectFolder = folderPickerConfig.getPathList();
        iHandlerCallBack = folderPickerConfig.getiHandlerCallBack();
        iHandlerCallBack.onStart();
        mContext = getContext();
        imageInfos = new ArrayList<>();

        mTextViewSelected.setText(mContext.getString(R.string.folder_picker_selected, 0));

        mTextViewSelected.setOnClickListener(view -> {
            if (!SearchCofig.BASE_SD_PATH.equals(currentFile.getAbsolutePath())) {
                mTextViewSelected.setText(currentFile.getParentFile().getName());
                FileService.getInstance().getFileList(currentFile.getParent());
                currentFile = currentFile.getParentFile();
            }
        });
        folderPickerAdapter = new FolderPickerAdapter(mContext, imageInfos);
        folderPickerAdapter.setHasPreviousBtn(true);
        folderPickerAdapter.setOnItemClickListener(new FolderPickerAdapter.OnItemClickListener() {
            @Override
            public void onImageClick(List<MediaEntity> selectImage) {
                mTextViewSelected.setText(mContext.getString(R.string.folder_picker_selected, selectImage.size()));
                iHandlerCallBack.onSuccess(selectImage);
                FolderPickerFragment.this.selectFolder = selectImage;
            }

            @Override
            public void onNextFolder(final String nextPath) {
                currentFile = new File(nextPath);
                mTextViewSelected.setText(currentFile.getName());
                FileService.getInstance().getFileList(nextPath);
            }

            @Override
            public void onPreviousFolder(String previousPaht) {
                if (!SearchCofig.BASE_SD_PATH.equals(currentFile.getAbsolutePath())) {
                    FileService.getInstance().getFileList(currentFile.getParent());
                    currentFile = currentFile.getParentFile();
                }
            }
        });
        mRecyclerViewImageList.setAdapter(folderPickerAdapter);

        AndroidPermission androidPermission = new AndroidPermission.Builder()
                .with(this)
                .permission(Permission.READ_EXTERNAL_STORAGE,
                        Permission.WRITE_EXTERNAL_STORAGE)
                .callback(new Callback() {
                    @Override
                    public void success(Activity permissionActivity) {
                        searchFile();
                    }

                    @Override
                    public void failure(Activity permissionActivity) {
                        Toast.makeText(mContext, "请开启文件读写权限！", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void nonExecution(Activity permissionActivity) {
                        //权限已通过，请求未执行
                        searchFile();
                    }
                })
                .build();
        androidPermission.execute();
    }

    private void searchFile() {
        MediaStoreUtil.setListener(MediaEntity.MEDIA_FOLDER, new MediaStoreUtil.FolderListener() {

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onFolderChange(int imageCount, List<MediaEntity> folders) {
                imageInfos.clear();
                imageInfos.addAll(folders);
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> folderPickerAdapter.notifyDataSetChanged());
            }
        });
        FileService.getInstance().getFileList(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MediaStoreUtil.removeListener(MediaEntity.MEDIA_FOLDER);
    }
}
