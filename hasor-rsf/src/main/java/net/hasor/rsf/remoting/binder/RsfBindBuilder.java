/*
 * Copyright 2008-2009 the original 赵永春(zyc@hasor.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.rsf.remoting.binder;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.more.util.StringUtils;
import net.hasor.core.Hasor;
import net.hasor.core.Provider;
import net.hasor.core.binder.InstanceProvider;
import net.hasor.rsf.RsfBinder;
import net.hasor.rsf.RsfFilter;
import net.hasor.rsf.RsfService;
import net.hasor.rsf.RsfSettings;
import net.hasor.rsf.adapter.AbstractRsfContext;
import net.hasor.rsf.constants.RsfException;
import net.hasor.rsf.domain.ServiceDomain;
/**
 * 服务注册器
 * @version : 2014年11月12日
 * @author 赵永春(zyc@hasor.net)
 */
public class RsfBindBuilder implements RsfBinder {
    private final AbstractRsfContext               rsfContext;
    private final Map<String, Provider<RsfFilter>> parentFilterMap;
    //
    protected RsfBindBuilder(AbstractRsfContext rsfContext) {
        this.rsfContext = rsfContext;
        this.parentFilterMap = new LinkedHashMap<String, Provider<RsfFilter>>();
    }
    protected AbstractRsfContext getContext() {
        return this.rsfContext;
    }
    public void bindFilter(String id, RsfFilter instance) {
        this.parentFilterMap.put(id, new InstanceProvider<RsfFilter>(instance));
    }
    public void bindFilter(String id, Provider<RsfFilter> provider) {
        this.parentFilterMap.put(id, provider);
    }
    public <T> LinkedBuilder<T> rsfService(Class<T> type) {
        return new LinkedBuilderImpl<T>(type, getContext());
    }
    public <T> NamedBuilder<T> rsfService(Class<T> type, T instance) {
        return this.rsfService(type).toInstance(instance);
    }
    public <T> NamedBuilder<T> rsfService(Class<T> type, Class<? extends T> implementation) {
        return this.rsfService(type).to(implementation);
    }
    public <T> NamedBuilder<T> rsfService(Class<T> type, Provider<T> provider) {
        return this.rsfService(type).toProvider(provider);
    }
    //
    public class LinkedBuilderImpl<T> implements LinkedBuilder<T> {
        private String                           serviceName;   //服务名
        private String                           serviceGroup;  //服务分组
        private String                           serviceVersion; //服务版本
        private int                              clientTimeout; //调用超时（毫秒）
        private String                           serializeType; //传输序列化类型
        private Class<T>                         serviceType;   //服务接口类型
        private Map<String, Provider<RsfFilter>> meFilterMap;
        private Provider<T>                      rsfProvider;
        private List<URL>                        addressList;
        private AbstractRsfContext               rsfContext;
        //
        protected LinkedBuilderImpl(Class<T> serviceType, AbstractRsfContext rsfContext) {
            RsfSettings settings = rsfContext.getSettings();
            this.rsfContext = rsfContext;
            //
            this.serviceName = serviceType.getName();
            this.serviceGroup = settings.getDefaultGroup();
            this.serviceVersion = settings.getDefaultVersion();
            this.clientTimeout = settings.getDefaultTimeout();
            this.serializeType = settings.getDefaultSerializeType();
            this.serviceType = serviceType;
            this.meFilterMap = new LinkedHashMap<String, Provider<RsfFilter>>(parentFilterMap);
            this.addressList = new ArrayList<URL>();
            //覆盖
            RsfService serviceInfo = serviceType.getAnnotation(RsfService.class);
            if (serviceInfo != null) {
                if (StringUtils.isBlank(serviceInfo.group()) == false)
                    this.serviceGroup = serviceInfo.group();
                if (StringUtils.isBlank(serviceInfo.name()) == false)
                    this.serviceName = serviceInfo.name();
                if (StringUtils.isBlank(serviceInfo.version()) == false)
                    this.serviceVersion = serviceInfo.version();
                if (StringUtils.isBlank(serviceInfo.serializeType()) == false)
                    this.serializeType = serviceInfo.serializeType();
                if (serviceInfo.clientTimeout() > 0)
                    this.clientTimeout = serviceInfo.clientTimeout();
            }
        }
        public ConfigurationBuilder<T> ngv(String name, String group, String version) {
            Hasor.assertIsNotNull(name, "name is null.");
            Hasor.assertIsNotNull(group, "group is null.");
            Hasor.assertIsNotNull(version, "version is null.");
            this.serviceName = name;
            this.serviceGroup = group;
            this.serviceVersion = version;
            return this;
        }
        public ConfigurationBuilder<T> timeout(int clientTimeout) {
            this.clientTimeout = clientTimeout;
            return this;
        }
        public ConfigurationBuilder<T> serialize(String serializeType) {
            Hasor.assertIsNotNull(serializeType, "serializeType is null.");
            this.serializeType = serializeType;
            return this;
        }
        public ConfigurationBuilder<T> bindFilter(String id, RsfFilter instance) {
            this.meFilterMap.put(id, new InstanceProvider<RsfFilter>(instance));
            return this;
        }
        public ConfigurationBuilder<T> bindFilter(String id, Provider<RsfFilter> provider) {
            if (provider != null) {
                this.meFilterMap.put(id, provider);
            }
            return this;
        }
        public NamedBuilder<T> to(final Class<? extends T> implementation) {
            return this.toProvider(new Provider<T>() {
                public T get() {
                    try {
                        return implementation.newInstance();
                    } catch (Exception e) {
                        throw new RsfException((short) 0, e);
                    }
                }
            });
        }
        public NamedBuilder<T> toInstance(T instance) {
            return this.toProvider(new InstanceProvider<T>(instance));
        }
        public NamedBuilder<T> toProvider(Provider<T> provider) {
            this.rsfProvider = provider;
            return this;
        }
        public RegisterReference<T> register() {
            ServiceDomain<T> domain = new ServiceDomain<T>(this.serviceType);
            domain.setBindName(this.serviceName);
            domain.setBindGroup(this.serviceGroup);
            domain.setBindVersion(this.serviceVersion);
            domain.setClientTimeout(this.clientTimeout);
            domain.setSerializeType(this.serializeType);
            //
            ServiceDefine<T> define = new ServiceDefine<T>(domain, this.rsfContext, this.meFilterMap, this.rsfProvider);
            //
            this.rsfContext.getBindCenter().publishService(define);
            this.rsfContext.getAddressCenter().updateStaticAddress(define, this.addressList);;
            return define;
        }
        public RegisterBuilder<T> addBindAddress(String hostIP, int hostPort) {
            try {
                String rsfPath = this.serviceGroup + "/" + this.serviceName + "/" + this.serviceVersion;
                this.addressList.add(new URL("rsf", hostIP, hostPort, rsfPath, new RsfURLStreamHandler()));
            } catch (Exception e) {}
            return this;
        }
    }
}