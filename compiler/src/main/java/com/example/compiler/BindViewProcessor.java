package com.example.compiler;

import com.example.annotation.BindView;
import com.example.compiler.utils.Constants;
import com.example.compiler.utils.EmptyUtils;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;


//用来生成META-INF/services/javax.annotation/processing.Processor 文件
@AutoService(Processor.class)
//允许/支持的注解类型，让注解处理器处理
@SupportedAnnotationTypes(Constants.BINDVIEW_ANNOTATION_TYPES)
//指定JDK编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class BindViewProcessor extends AbstractProcessor {
    //操作Elements工具类（类、函数、属性都是Elements）
    private Elements elementUtils;
    //Messager用来报告错误，警告和其他提示信息
    private Messager messager;
    //文件生成器 类/资源，Filer用来创建新的类文件，class文件以及辅助文件
    private Filer filer;
//key：类节点  value：被@BindView注解的属性的结合（缓存）
    private Map<TypeElement, List<Element>> tempBindViewMap = new HashMap<>();

    /**
     * 该方法主要用于一些初始化操作，通过该方法的参数ProcessingEnvironment可以获取到一些有用的工具类
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        //初始化工作
        elementUtils = processingEnvironment.getElementUtils();
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
        messager.printMessage(Diagnostic.Kind.NOTE,"注解处理器初始化完成，开始处理注解----------");
    }

  /*  @Override
    public Set<String> getSupportedAnnotationTypes() {
        return super.getSupportedAnnotationTypes();
        //支持的允许的注解处理类型，不加支持的话不会进“process“方法


    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return super.getSupportedSourceVersion();
        //指定jdk的编译版本
    }

    @Override
    public Set<String> getSupportedOptions() {
        return super.getSupportedOptions();
        //接收参数
    }*/

    /**
     * 相当于main函数，开始处理注解
     * 注解处理器的核心方法，处理具体的注解，生成java文件
     * @param set  使用了支出处理器的节点的集合
     * @param roundEnvironment 当前或是之前的运行环境，可以通过该对象查找的注解
     * @return true 表示后续处理器不会再继续处理（已经处理完成）
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //一旦有在方法之上使用了@BindView 注解的元素集合
       if(!EmptyUtils.isEmpty(set)){
           //获取所有被@BindView注解的元素集合
           Set<? extends Element> bindViewElements = roundEnvironment.getElementsAnnotatedWith(BindView.class);
           if(!EmptyUtils.isEmpty(bindViewElements)){
               try {
                   //赋值临时map存储，用来存放被注解的属性的集合
                   valueOfMap(bindViewElements);

                   //生成类文件
                 createJavaFile(bindViewElements);
                   return true;

               }catch (Exception e){
                   e.printStackTrace();
               }
           }

       }
        return false;
    }

    private void createJavaFile(Set<? extends Element> elements) throws IOException {
        //判断是否有要生成的类文件
        if(EmptyUtils.isEmpty(tempBindViewMap)){

            return;
        }

        //获取ViewBinder接口类型（生成类文件需要实现的接口）
        TypeElement viewBinderType = elementUtils.getTypeElement(Constants.VIEWBINDER);
        for (Map.Entry<TypeElement, List<Element>> entry : tempBindViewMap.entrySet()) {
            //类名
            ClassName className = ClassName.get(entry.getKey());
            //实现接口泛型
            ParameterizedTypeName typeName = ParameterizedTypeName.get(ClassName.get(viewBinderType),
                    ClassName.get(entry.getKey()));
            //参数体配置
            ParameterSpec parameterSpec = ParameterSpec.builder(ClassName.get(entry.getKey())
                    ,Constants.TARGET_PARAMETER_NAME)
                    .build();
            //方法配置
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Constants.BIND_METHOD_NAME)
                    .addAnnotation(Override.class)//重写注释
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(parameterSpec);
            for (Element fieldElement : entry.getValue()) {
                //获取属性名
                String fieldName =  fieldElement.getSimpleName().toString();
                //获取@BIndView的注解值
                int annotationValue = fieldElement.getAnnotation(BindView.class).value();
                String methodContent = "$N."+fieldName+" = $N.findViewById($L)";
                methodBuilder.addStatement(methodContent,
                        Constants.TARGET_PARAMETER_NAME
                ,Constants.TARGET_PARAMETER_NAME
                ,annotationValue);
            }

            //必须是同包（属性修饰符缺省）
            JavaFile.builder(className.packageName(),
                    TypeSpec.classBuilder(className.simpleName()+"$$ViewBinder")
                            .addSuperinterface(typeName)
                            .addModifiers(Modifier.PUBLIC)
                            .addMethod(methodBuilder.build())
                            .build())
                    .build()
                    .writeTo(filer);
        }
    }

    private void valueOfMap(Set<? extends Element> bindViewElements) {
        if(!EmptyUtils.isEmpty(bindViewElements)){
            for (Element element : bindViewElements) {
                //注解在属性之上，属性节点父节点是类节点
                TypeElement typeElement = (TypeElement) element.getEnclosingElement();
                //如果map集合中的key：类节点存在，直接添加属性
                if(tempBindViewMap.containsKey(typeElement)){
                    tempBindViewMap.get(typeElement).add(element);
                }else {
                    List<Element> fields = new ArrayList<>();
                    fields.add(element);
                    tempBindViewMap.put(typeElement,fields);
                }
            }
        }
    }

    private void parseElements(Set<? extends Element> elements) throws IOException {
        //方法体
        MethodSpec main = MethodSpec.methodBuilder("main") //方法名
                .addModifiers(Modifier.PUBLIC,Modifier.STATIC) //方法修饰符
                .returns(void.class) //方法返回值（默认 void）
                .addParameter(String[].class,"args") //方法参数
                .addStatement("$T.out.println($S)",System.class,"Hello bing") //方法内容
                .build();  //构建
        //类
        TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorldaa") //类名
                .addModifiers(Modifier.PUBLIC,Modifier.FINAL)  //类修饰符
                .addMethod(main)//加入方法体
                .build();//构建
        //文件生成器
        JavaFile javaFile = JavaFile.builder("com.example.helloworld",helloWorld)
                .build();
        javaFile.writeTo(filer);

    }
}
