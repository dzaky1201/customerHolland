package com.dzakyhdr.fixholand.Callback;

import com.dzakyhdr.fixholand.Model.CommentModel;

import java.util.List;

public interface ICommentCallbackListener {
    void onCommentLoadSuccess(List<CommentModel>commentModels);
    void onCommentLoadFailed(String message);
}
