package com.heima.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private ArticleFreemarkerService articleFreemarkerService;

    private final static short MAX_PAGE_SIZE = 50;

    /**
     * 加载文章列表
     * @param loadType 1为加载更多  2为加载最新
     * @param dto
     * @return
     */
    @Override
    public ResponseResult load(ArticleHomeDto dto, Short loadType) {
        //1.校验参数
        //分页条数的校验
        Integer size = dto.getSize();
        if (size == null || size == 0) {
            size = 10;
        }
        //分页的值不超过50
        size = Math.min(size, MAX_PAGE_SIZE);
        dto.setSize(size);

        //校验参数 -->type
        if (!loadType.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !loadType.equals(ArticleConstants.LOADTYPE_LOAD_NEW)) {
            loadType = 1;
        }

        //频道参数校验
        if (StringUtils.isBlank(dto.getTag())) {
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }

        //时间校验
        if (dto.getMaxBehotTime() == null) {
            dto.setMaxBehotTime(new Date());
        }
        if (dto.getMinBehotTime() == null) {
            dto.setMinBehotTime(new Date());
        }
        //2.查询
        List<ApArticle> articleList = apArticleMapper.loadArticleList(dto, loadType);
        //3.结果返回
        return ResponseResult.okResult(articleList);
    }

    /**
     * 保存app端相关文章
     *
     * @param dto
     * @return
     */
    @Override
//    @Async //异步调用
    public ResponseResult saveArticle(ArticleDto dto) {
        //1.检查参数
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        ApArticle article = new ApArticle();
        BeanUtils.copyProperties(dto, article);
        //2.判断是否存在id
        if (dto.getId() == null) {
            //2.1. 不存在id-->保存文章，文章配置，文章内容表
            //保存文章
            save(article);
            //保存配置
            ApArticleConfig apArticleConfig = new ApArticleConfig(article.getId());
            apArticleConfigMapper.insert(apArticleConfig);
            //保存文章内容
            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setArticleId(article.getId());
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.insert(apArticleContent);
        }
        else {
            //2.2. 存在id-->修改文章，文章内容表
            //修改文章表
            updateById(article);
            //修改文章内容
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, dto.getId()));
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.updateById(apArticleContent);
        }

        //异步调用 生成静态文件并上传到minIO中
        articleFreemarkerService.buildArticleToMinIO(article, dto.getContent());

        //3.结果返回-->文章id
        return ResponseResult.okResult(article.getId());
    }
}
