package life.tannineo.cs7ns6.util;


import java.io.*;

public class CloneUtil {
    public static <T extends Serializable> T clone(T obj) {
        T cloneObj = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());//获取上面的输出字节流
            ObjectInputStream ois = new ObjectInputStream(bais);

            cloneObj = (T) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cloneObj;
    }
}
