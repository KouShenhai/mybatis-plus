/*
 * Copyright (c) 2011-2024, baomidou (jobob@qq.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baomidou.mybatisplus.generator.index;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.builder.Entity;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.jdbc.DatabaseMetaDataWrapper;
import com.baomidou.mybatisplus.generator.model.MapperMethod;
import com.baomidou.mybatisplus.generator.util.KotlinTypeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 按字符串或者字符串常量方法生成查询条件
 *
 * @author nieqiurong
 * @see Entity.Builder#enableColumnConstant()
 * @since 3.5.10
 */
public class DefaultGenerateMapperMethodHandler extends AbstractMapperMethodHandler {

    @Override
    public List<MapperMethod> getMethodList(TableInfo tableInfo) {
        Map<String, List<DatabaseMetaDataWrapper.Index>> indexlistMap = tableInfo.getIndexList().stream()
            .collect(Collectors.groupingBy(DatabaseMetaDataWrapper.Index::getName));
        String entityName = tableInfo.getEntityName();
        GlobalConfig globalConfig = tableInfo.getGlobalConfig();
        Entity entity = tableInfo.getStrategyConfig().entity();
        boolean columnConstant = entity.isColumnConstant();
        Set<Map.Entry<String, List<DatabaseMetaDataWrapper.Index>>> entrySet = indexlistMap.entrySet();
        List<MapperMethod> methodList = new ArrayList<>();
        for (Map.Entry<String, List<DatabaseMetaDataWrapper.Index>> entry : entrySet) {
            String indexName = entry.getKey();
            List<DatabaseMetaDataWrapper.Index> indexList = entry.getValue();
            int indexSize = indexList.size();
            if ("PRIMARY".equals(indexName)) {
                if (indexSize == 1) {
                    continue;
                }
            }
            List<TableField> tableFieldList = new ArrayList<>();
            Map<String, TableField> tableFieldMap = tableInfo.getTableFieldMap();
            StringBuilder baseMethodNameBuilder = new StringBuilder();
            StringBuilder argsBuilder = new StringBuilder();
            StringBuilder baseWrapperBuilder = new StringBuilder();
            boolean uniqueKey = false;
            for (int i = 0; i < indexSize; i++) {
                DatabaseMetaDataWrapper.Index index = indexList.get(i);
                if (index.isUnique()) {
                    uniqueKey = true;
                }
                TableField tableField = tableFieldMap.get(index.getColumnName());
                tableFieldList.add(tableField);
                baseMethodNameBuilder.append(tableField.getCapitalName());
                if (indexSize > 1) {
                    if (columnConstant) {
                        baseWrapperBuilder.append("eq(ObjectUtils.isNotNull(").append(tableField.getPropertyName()).append(")").append(", ").append(entityName).append(".").append(tableField.getName().toUpperCase());
                    } else {
                        baseWrapperBuilder.append("eq(ObjectUtils.isNotNull(").append(tableField.getPropertyName()).append(")").append(", ").append("\"").append(tableField.getColumnName()).append("\"");
                    }
                } else {
                    if (columnConstant) {
                        baseWrapperBuilder.append("eq(").append(entityName).append(".").append(tableField.getName().toUpperCase());
                    } else {
                        baseWrapperBuilder.append("eq(").append("\"").append(tableField.getColumnName()).append("\"");
                    }
                }
                baseWrapperBuilder.append(",").append(" ").append(tableField.getPropertyName()).append(")");
                if (globalConfig.isKotlin()) {
                    argsBuilder.append(tableField.getPropertyName()).append(":").append(" ")
                        .append(KotlinTypeUtils.getStringType(tableField.getColumnType()));
                } else {
                    argsBuilder.append(tableField.getColumnType().getType()).append(" ").append(tableField.getPropertyName());
                }
                if (i < indexSize - 1) {
                    baseWrapperBuilder.append(".");
                    baseMethodNameBuilder.append("And");
                    argsBuilder.append(", ");
                }
            }
            String baseMethodName = baseMethodNameBuilder.toString();
            String args = argsBuilder.toString();
            String baseWrapper = baseWrapperBuilder.toString();
            boolean returnList = (indexSize > 1 || !uniqueKey);
            if (globalConfig.isKotlin()) {
                String selectByMethod;
                if (returnList) {
                    selectByMethod = buildKotlinMethod("selectBy" + baseMethodName, args,
                        "List<" + tableInfo.getEntityName() + ">?", "selectList(Wrappers.query<" + tableInfo.getEntityName() + ">()." + baseWrapper + ")");
                } else {
                    selectByMethod = buildKotlinMethod("selectBy" + baseMethodName, args,
                        tableInfo.getEntityName() + "?", "selectOne(Wrappers.query<" + tableInfo.getEntityName() + ">()." + baseWrapper + ")");
                }
                String updateByMethod = buildKotlinMethod("updateBy" + baseMethodName, "entity:" + " " + tableInfo.getEntityName() + ", " + args,
                    "Int", "update(entity, Wrappers.update<" + tableInfo.getEntityName() + ">()." + baseWrapper + ")");
                String deleteByMethod = buildKotlinMethod("deleteBy" + baseMethodName, args,
                    "Int", "delete(Wrappers.update<" + tableInfo.getEntityName() + ">()." + baseWrapper + ")");
                methodList.add(new MapperMethod(indexName, selectByMethod, tableFieldList));
                methodList.add(new MapperMethod(indexName, updateByMethod, tableFieldList));
                methodList.add(new MapperMethod(indexName, deleteByMethod, tableFieldList));
            } else {
                String selectByMethod;
                if (returnList) {
                    selectByMethod = buildMethod("selectBy" + baseMethodName, args, "List<" + tableInfo.getEntityName() + ">",
                        "selectList(Wrappers.<" + tableInfo.getEntityName() + ">query()." + baseWrapper + ")");
                } else {
                    selectByMethod = buildMethod("selectBy" + baseMethodName, args, tableInfo.getEntityName(),
                        "selectOne(Wrappers.<" + tableInfo.getEntityName() + ">query()." + baseWrapper + ")");
                }

                String updateByMethod = buildMethod(
                    "updateBy" + baseMethodName, tableInfo.getEntityName() + " entity" + ", " + args, "int",
                    "update(entity, Wrappers.<" + tableInfo.getEntityName() + ">update()." + baseWrapper + ")");
                String deleteByMethod = buildMethod("deleteBy" + baseMethodName, args, "int",
                    "delete(Wrappers.<" + tableInfo.getEntityName() + ">update()." + baseWrapper + ")");
                methodList.add(new MapperMethod(indexName, selectByMethod, tableFieldList));
                methodList.add(new MapperMethod(indexName, updateByMethod, tableFieldList));
                methodList.add(new MapperMethod(indexName, deleteByMethod, tableFieldList));
            }
        }
        return methodList;
    }

    @Override
    public Set<String> getImportPackages(TableInfo tableInfo) {
        Set<String> imports = new HashSet<>();
        imports.add(ObjectUtils.class.getName());
        imports.add(List.class.getName());
        imports.add(Wrappers.class.getName());
        return imports;
    }

}
