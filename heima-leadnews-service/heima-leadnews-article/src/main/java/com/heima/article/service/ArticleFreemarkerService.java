package com.heima.article.service;

import com.heima.model.article.pojos.ApArticle;

public interface ArticleFreemarkerService {


    /**
     * 生成静态文件上传到minIO中
     * @param article
     * @param content
     */
    public void buildArticleToMinIO(ApArticle article, String content);
}
