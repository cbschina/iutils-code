package cn.iutils.code.service;

import cn.iutils.code.config.Config;
import cn.iutils.code.entity.ColumnModel;
import cn.iutils.code.entity.TableModel;
import cn.iutils.code.utils.ConfigurationHelper;
import cn.iutils.code.utils.StringUtils;
import cn.iutils.code.utils.db.DBMangerPool;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.FileUtils;

import javax.swing.tree.TreePath;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成服务
 */
public class CodeService {

    /**
     * freemaker 配置
     */
    private Configuration cfg;

    /**
     * 操作池
     */
    DBMangerPool dBMangerPool = DBMangerPool.getInstance();

    /**
     * 自动生成代码
     * @param treePath 树
     * @param myPackage 包名
     * @param model 模块
     * @param subModel 子模块
     * @return
     */
    public String auotoCode(TreePath[] treePath,String myPackage,String model,String subModel){
        String msg = null;
        try {
            // 获得freemaker 配置
            cfg = ConfigurationHelper
                    .getConfiguration(Config.templatePath);
            String key, catalog, table, tableDesc = "", className = "";// 获取操作类的key、数据库目录、表名
            List<ColumnModel> columns = null;
            for (int i = 0; i < treePath.length; i++) {
                TreePath tmp = treePath[i];
                if (tmp.getPath().length < 3)
                    continue;
                key = Config.dbKey;
                catalog = tmp.getPath()[1].toString();
                table = tmp.getPath()[2].toString();
                tableDesc = table.substring(table.indexOf("(") + 1,
                        table.lastIndexOf(")"));
                table = table.substring(0, table.indexOf("("));
                columns = dBMangerPool.getDBManger(key).getColumnsByTable(
                        catalog, table);
                className = StringUtils.toClassName(table);
                shunt(className,myPackage,model,subModel, tableDesc, columns, table);
            }
            msg = "生成成功";
        } catch (Exception e) {
            e.printStackTrace();
            msg = e.getMessage();
        }
        return msg;
    }

    /**
     * 多文件生成
     * @param className 类名
     * @param myPackage 包名
     * @param model 模块
     * @param subModel 子模块
     * @param tableDesc 表描述
     * @param columns 列
     * @param table 表
     * @throws Exception
     */
    public void shunt(String className,String myPackage,String model,String subModel, String tableDesc,
                        List<ColumnModel> columns, String table) throws Exception {
        // 装载model数据
        TableModel tableModel = new TableModel();
        tableModel.setPackageName(myPackage);
        tableModel.setModel(model);
        tableModel.setSubModel(subModel);
        tableModel.setClassName(className);
        tableModel.setColumns(columns);
        tableModel.setTableDesc(tableDesc);
        tableModel.setTableName(table);

        // 开始生成实体类
        save(tableModel, "Pojo.java.ftl", "/" + className + "/",
                className + ".java");
        // 开始生成Dao接口
        save(tableModel, "Dao.java.ftl", "/" + className + "/",
                className + "Dao.java");
        // 开始生成Dao.xml文件
        save(tableModel, "Mapper.xml.ftl", "/" + className + "/",
                className + "Dao.xml");
        // 开始生成Service
        save(tableModel, "Service.java.ftl", "/" + className + "/",
                className + "Service.java");
        // 开始生成Controller
        save(tableModel, "Controller.java.ftl", "/" + className
                + "/", className + "Controller.java");
        // 开始生成页面
        save(tableModel, "Form.jsp.ftl", "/" + className + "/",
                "form.jsp");
        save(tableModel, "List.jsp.ftl", "/" + className + "/",
                "list.jsp");
    }

    /**
     * 保存到文件
     *
     * @return
     * @throws Exception
     */
    public void save(TableModel tableModel, String templateName, String path,
                       String fileName) throws Exception {
        // 装载model数据
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("model", tableModel);
        // 获得模板对象
        Template template = cfg.getTemplate(templateName);
        //设置编码
        template.setEncoding("UTF-8");
        // 创建生成类的存放路径
        FileUtils.forceMkdir(new File(Config.outCodePath + path));
        File output = new File(Config.outCodePath + path, fileName);
        FileOutputStream fos= new FileOutputStream(output);
        OutputStreamWriter osw =new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter bw =new BufferedWriter(osw, 1024);
        // 开始创建
        template.process(data, bw);
    }


}
