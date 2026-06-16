package com.mega.lab.widget.dto;

/**
 * 接口投影（interface-based projection）。
 *
 * 你只声明要哪几列的 getter，Spring Data 运行期生成代理，并且**只 SELECT 这几列**
 * （不是查出整个实体再丢字段）。适合列表页只要少数字段、不想拖出大对象的场景。
 *
 * getter 名要和实体属性名对应：getSku() ↔ Widget.sku，getQuantity() ↔ Widget.quantity。
 */
public interface WidgetSummary {
    String getSku();
    int getQuantity();
}
