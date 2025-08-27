package cn.cordys.mybatis.lambda;

import cn.cordys.common.exception.GenericException;

import java.io.*;

/**
 * 当前类是 {@link java.lang.invoke.SerializedLambda} 的一个镜像。
 * <p>
 * 该类用于序列化并提取 Lambda 表达式的元数据，提供了与原 {@link SerializedLambda} 类相同的功能，
 * 但可以在自定义序列化环境中使用。
 * </p>
 */
@SuppressWarnings("ALL")
public class SerializedLambda implements Serializable {
    private static final long serialVersionUID = 8025925345765570181L;

    private Class<?> capturingClass;
    private String functionalInterfaceClass;
    private String functionalInterfaceMethodName;
    private String functionalInterfaceMethodSignature;
    private String implClass;
    private String implMethodName;
    private String implMethodSignature;
    private int implMethodKind;
    private String instantiatedMethodType;
    private Object[] capturedArgs;

    /**
     * 从序列化的 Lambda 表达式中提取 {@link SerializedLambda} 对象。
     * 该方法通过序列化和反序列化过程提取 Lambda 表达式的元信息。
     *
     * @param serializable 可序列化对象，通常为 Lambda 表达式。
     * @return 提取的 {@link SerializedLambda} 对象。
     * @throws GenericException 如果序列化或反序列化过程中出现异常。
     */
    public static SerializedLambda extract(Serializable serializable) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(serializable);
            oos.flush();

            try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                    Class<?> clazz = super.resolveClass(desc);
                    return clazz == java.lang.invoke.SerializedLambda.class ? SerializedLambda.class : clazz;
                }
            }) {
                return (SerializedLambda) objectInputStream.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new GenericException("提取 Lambda 表达式时发生异常", e);
        }
    }

    /**
     * 获取实例化方法类型的字符串。
     *
     * @return 实例化方法类型的字符串。
     */
    public String getInstantiatedMethodType() {
        return instantiatedMethodType;
    }

    /**
     * 获取捕获类的 {@link Class} 对象。
     *
     * @return 捕获类的 {@link Class} 对象。
     */
    public Class<?> getCapturingClass() {
        return capturingClass;
    }

    /**
     * 获取实现方法的名称。
     *
     * @return 实现方法的名称。
     */
    public String getImplMethodName() {
        return implMethodName;
    }
}
