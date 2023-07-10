package com.heima.article.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class ArticleFreemarkerServiceImpl implements ArticleFreemarkerService {

    @Autowired
    private Configuration configuration;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ApArticleMapper apArticleMapper;

    /**
     * 生成静态文件上传到minIO中
     *
     * @param article
     * @param content
     */
    @Override
    public void buildArticleToMinIO(ApArticle article, String content) {
        //1.获取文章内容
        if (StringUtils.isNotBlank(content)) {
            //2.文章内容通过freemarker生成html文件
            StringWriter out = new StringWriter();
            Template template = null;
            try {
                template = configuration.getTemplate("article.ftl");
                //数据模型
                Map<String, Object> params = new HashMap<>();
                params.put("content", JSONArray.parseArray(content));
                //合成
                template.process(params, out);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //将out转换成一个inputStream
            InputStream is = new ByteArrayInputStream(out.toString().getBytes());

            //3.把html文件上传到minio中
            String path = fileStorageService.uploadHtmlFile("", article.getId() + ".html", is);

            //4.修改ap_article表，保存static_url字段
            //4.4 修改ap_article表，保存static_url字段
            article.setStaticUrl(path);
            apArticleMapper.updateById(article);
        }
    }
}
