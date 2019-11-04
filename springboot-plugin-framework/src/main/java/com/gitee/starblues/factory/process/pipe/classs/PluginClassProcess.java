package com.gitee.starblues.factory.process.pipe.classs;

import com.gitee.starblues.extension.ExtensionFactory;
import com.gitee.starblues.extension.ExtensionInitializer;
import com.gitee.starblues.factory.PluginRegistryInfo;
import com.gitee.starblues.factory.process.pipe.PluginPipeProcessor;
import com.gitee.starblues.factory.process.pipe.classs.group.*;
import com.gitee.starblues.loader.PluginResourceLoadFactory;
import com.gitee.starblues.loader.ResourceWrapper;
import com.gitee.starblues.loader.load.PluginClassLoader;
import com.gitee.starblues.realize.BasePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件类加载处理者
 *
 * @author zhangzhuo
 * @version 2.1.0
 */
public class PluginClassProcess implements PluginPipeProcessor {


    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * 其他类
     */
    public static final String OTHER = "other";


    private final List<PluginClassGroup> pluginClassGroups = new ArrayList<>();


    public PluginClassProcess(){}


    @Override
    public void initialize() {
        pluginClassGroups.add(new ComponentGroup());
        pluginClassGroups.add(new ControllerGroup());
        pluginClassGroups.add(new RepositoryGroup());
        pluginClassGroups.add(new ConfigurationGroup());
        pluginClassGroups.add(new ConfigDefinitionGroup());
        pluginClassGroups.add(new SupplierGroup());
        pluginClassGroups.add(new CallerGroup());
        // 添加扩展
        pluginClassGroups.addAll(ExtensionInitializer.getClassGroupExtends());



    }

    @Override
    public void registry(PluginRegistryInfo pluginRegistryInfo) throws Exception {
        BasePlugin basePlugin = pluginRegistryInfo.getBasePlugin();
        PluginResourceLoadFactory pluginResourceLoadFactory = basePlugin.getPluginResourceLoadFactory();
        ResourceWrapper resourceWrapper = pluginResourceLoadFactory.getPluginResources(PluginClassLoader.KEY);
        if(resourceWrapper == null){
            return;
        }
        List<Resource> pluginResources = resourceWrapper.getResources();
        if(pluginResources == null){
            return;
        }
        for (PluginClassGroup pluginClassGroup : pluginClassGroups) {
            try {
                pluginClassGroup.initialize(basePlugin);
            } catch (Exception e){
                log.error("PluginClassGroup {} initialize exception. {}", pluginClassGroup.getClass(),
                        e.getMessage(), e);
            }
        }
        for (Resource pluginResource : pluginResources) {
            String path = pluginResource.getURL().getPath();
            String packageName = path.substring(0, path.indexOf(".class"))
                    .replace("/", ".");
            packageName = packageName.substring(packageName.indexOf(basePlugin.scanPackage()));
            Class<?> aClass = Class.forName(packageName, false,
                    basePlugin.getWrapper().getPluginClassLoader());
            if(aClass == null){
                continue;
            }
            boolean findGroup = false;
            for (PluginClassGroup pluginClassGroup : pluginClassGroups) {
                if(pluginClassGroup == null || StringUtils.isEmpty(pluginClassGroup.groupId())){
                    continue;
                }
                if(pluginClassGroup.filter(aClass)){
                    pluginRegistryInfo.addGroupClasses(pluginClassGroup.groupId(), aClass);
                    findGroup = true;
                }
            }
            if(!findGroup){
                pluginRegistryInfo.addGroupClasses(OTHER, aClass);
            }
            pluginRegistryInfo.addClasses(aClass);
        }
    }

    @Override
    public void unRegistry(PluginRegistryInfo registerPluginInfo) throws Exception {
        registerPluginInfo.cleanClasses();
    }

}
