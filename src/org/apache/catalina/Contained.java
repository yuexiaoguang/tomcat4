package org.apache.catalina;


/**
 * <p>去耦接口，它指定一个实现类最多与一个 <strong>Container</strong> 实例相关联。</p>
 */

public interface Contained {


    //-------------------------------------------------------------- Properties


    /**
     * 返回当前实例关联的<code>Container</code>，如果存在; 否则返回<code>null</code>.
     */
    public Container getContainer();


    /**
     * 设置这个实例关联的<code>Container</code>
     *
     * @param container 被关联的Container实例
     */
    public void setContainer(Container container);


}
