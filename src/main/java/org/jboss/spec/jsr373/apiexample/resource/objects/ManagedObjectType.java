/*
 * JBoss, Home of Professional Open Source.
 * Copyright ${year}, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.spec.jsr373.apiexample.resource.objects;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.spec.jsr373.apiexample.resource.Attribute;
import org.jboss.spec.jsr373.apiexample.resource.AttributeType;
import org.jboss.spec.jsr373.apiexample.resource.ResourceInstance;
import org.jboss.spec.jsr373.apiexample.resource.ResourceTemplate;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class ManagedObjectType {
    public static final String STATE_MANAGEABLE = "stateManageable";
    public static final String STATISTICS_PROVIDER = "statisticsProvider";
    public static final String EVENT_PROVIDER = "eventProvider";

    private final String name;
    private final String path;
    private ResourceTemplate template;

    private static final Map<Class<? extends ManagedObjectType>, ManagedObjectType> INSTANCES;
    static {
        Map<Class<? extends ManagedObjectType>, ManagedObjectType> instances = new HashMap<>();
        INSTANCES = initialiseInstances();
    }

    protected ManagedObjectType(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public static ManagedObjectType getInstanceForClass(Class<? extends ManagedObjectType> type) {
        Map x = INSTANCES;
        return INSTANCES.get(type);
    }

    public final String getName() {
        return name;
    }

    public abstract String getDescription();

    public final String getPath() {
        return path;
    }

    public abstract Set<ManagedObjectType> getParents();

    public ResourceTemplate getTemplate() {
        return template;
    }

    public void addAttributeDescriptions(ResourceTemplate.Builder builder) {
        builder.addAttribute(
                Attribute.createBuilder(STATE_MANAGEABLE, AttributeType.BOOLEAN, "Whether the object is state manageable.")
                        .build());
        builder.addAttribute(
                Attribute.createBuilder(STATISTICS_PROVIDER, AttributeType.BOOLEAN, "Whether the object supports the generation of statistics.")
                        .build());
        builder.addAttribute(
                Attribute.createBuilder(EVENT_PROVIDER, AttributeType.BOOLEAN, "Whether the object is state manageable.")
                        .build());
    }

    public void setDefaultAttributeValues(ResourceInstance.Builder builder) {
        builder.setAttribute(STATE_MANAGEABLE, new ModelNode(false));
        builder.setAttribute(STATISTICS_PROVIDER, new ModelNode(false));
        builder.setAttribute(EVENT_PROVIDER, new ModelNode(false));
    }

    public void setTemplate(ResourceTemplate template) {
        if (this.template != null) {
            throw new IllegalStateException("Already built a template for " + name);
        }
        this.template = template;
    }

    static Set<ManagedObjectType> parents(ManagedObjectType...parents) {
        Set<ManagedObjectType> set = new HashSet<>();
        for (ManagedObjectType parent : parents) {
            set.add(parent);
        }
        return set;
    }

    private static Map<Class<? extends ManagedObjectType>, ManagedObjectType> initialiseInstances() {
        try {
            Map<Class<? extends ManagedObjectType>, ManagedObjectType> instances = new HashMap<>();
            Package pkg = ManagedObjectType.class.getPackage();
            URL url = ManagedObjectType.class.getResource(ManagedObjectType.class.getSimpleName() + ".class");
            File packageDir = new File(url.toURI()).getParentFile();
            for (File file : packageDir.listFiles()) {
                String name = file.getName();
                int index = name.indexOf(".class");
                if (index == -1) {
                    continue;
                }
                name = name.substring(0, index);
                Class<?> clazz = Class.forName(pkg.getName() + "." + name);
                if ((clazz.getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT) {
                    continue;
                }
                boolean inheritsManagedObjectType = false;
                Class<?> current = clazz;
                while (current != Object.class) {
                    if (current == ManagedObjectType.class) {
                        inheritsManagedObjectType = true;
                        break;
                    }
                    current = current.getSuperclass();
                }
                if (inheritsManagedObjectType) {
                    Field field = clazz.getDeclaredField("INSTANCE");
                    ManagedObjectType type = (ManagedObjectType)field.get(null);
                    instances.put((Class<? extends ManagedObjectType>)clazz, type);
                }
            }
            return instances;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void addType(Map<Class<? extends ManagedObjectType>, ManagedObjectType> instances, ManagedObjectType type) {
        instances.put(type.getClass(), type);
    }
}