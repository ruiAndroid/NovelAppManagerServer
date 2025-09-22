package com.fun.novel.service.impl;

import com.fun.novel.dto.CreateNovelAppRequest;
import com.fun.novel.service.NovelAppLocalFileOperationService;
import com.fun.novel.service.fileOpeartionService.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NovelAppLocalFileOperationServiceImpl extends AbstractConfigFileOperationService implements NovelAppLocalFileOperationService {



    @Autowired
    private BaseConfigFileOperationService baseConfigFileOperationService;

    @Autowired
    private AdConfigFileOperationService adConfigFileOperationService;

    @Autowired
    private PayConfigFileOperationService payConfigFileOperationService;

    @Autowired
    private CommonConfigFileOperationService commonConfigFileOperationService;

    @Autowired
    private AppConfigFileOperationService appConfigFileOperationService;

    @Autowired
    private PreFileOperationService preFileOperationService;

    @Autowired
    private UiConfigFileOperationService uiConfigFileOperationService;

    @Override
    public void createNovelAppLocalCodeFiles(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        doCreateLocalCodeFiles(taskId, params, rollbackActions);

    }


    @Override
    public void updateBaseConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        baseConfigFileOperationService.updateBaseConfigLocalCodeFiles(params, rollbackActions);


    }

    @Override
    public void updateAdConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        adConfigFileOperationService.updateAdConfigLocalCodeFiles(params, rollbackActions);
    }

    @Override
    public void updateCommonConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        commonConfigFileOperationService.updateCommonConfigLocalCodeFiles(params, rollbackActions);

    }

    @Override
    public void updatePayConfigLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions) {
        payConfigFileOperationService.updatePayConfigLocalCodeFiles(params, rollbackActions);

    }

    @Override
    public void createPayConfig(CreateNovelAppRequest createNovelAppRequest, List<Runnable> rollbackActions, String payType) {
        payConfigFileOperationService.createPayConfig(createNovelAppRequest, rollbackActions, payType);


    }

    @Override
    public void deletePayConfig(CreateNovelAppRequest params, List<Runnable> rollbackActions,String payType) {
        payConfigFileOperationService.deletePayConfig(params, rollbackActions,payType);

    }

    @Override
    public void deleteAppLocalCodeFiles(CreateNovelAppRequest params, List<Runnable> rollbackActions,boolean isLast) {
        preFileOperationService.deletePreBuildFiles(params, rollbackActions);
        preFileOperationService.deletePreFetchBuildFile(params, rollbackActions);

        baseConfigFileOperationService.deleteBaseConfigLocalCodeFiles(params, rollbackActions,isLast);
        appConfigFileOperationService.deleteAppConfigAndPackageFile(params, rollbackActions,isLast);

        adConfigFileOperationService.deleteAdConfigLocalCodeFiles(params, rollbackActions,isLast);
        commonConfigFileOperationService.deleteCommonConfigLocalCodeFiles(params, rollbackActions,isLast);
        payConfigFileOperationService.deletePayConfigLocalCodeFiles(params, rollbackActions,isLast);
        uiConfigFileOperationService.deleteUiConfigLocalCodeFiles(params, rollbackActions,isLast);


    }


    private void doCreateLocalCodeFiles(String taskId, CreateNovelAppRequest params, List<Runnable> rollbackActions) {

        baseConfigFileOperationService.createBaseConfigLocalCodeFiles(taskId, params, rollbackActions);
        preFileOperationService.createPreFiles(taskId, params, rollbackActions);
        adConfigFileOperationService.createAdConfigLocalCodeFiles(taskId, params, rollbackActions);
        payConfigFileOperationService.createPayConfigLocalCodeFiles(taskId,params, rollbackActions);
        commonConfigFileOperationService.createCommonConfigLocalCodeFiles(taskId,params, rollbackActions);

        appConfigFileOperationService.createAppConfigAndPackageFile(taskId,params, rollbackActions);
        uiConfigFileOperationService.createUiConfigLocalCodeFiles(taskId,params, rollbackActions);

    }

}