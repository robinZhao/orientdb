/* Generated By:JJTree: Do not edit this line. OUpdateItem.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.OElement;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;

import java.util.*;
import java.util.stream.Collectors;

public class OUpdateItem extends SimpleNode {
  public static final int OPERATOR_EQ          = 0;
  public static final int OPERATOR_PLUSASSIGN  = 1;
  public static final int OPERATOR_MINUSASSIGN = 2;
  public static final int OPERATOR_STARASSIGN  = 3;
  public static final int OPERATOR_SLASHASSIGN = 4;

  protected OIdentifier left;
  protected OModifier   leftModifier;
  protected int         operator;
  protected OExpression right;

  public OUpdateItem(int id) {
    super(id);
  }

  public OUpdateItem(OrientSql p, int id) {
    super(p, id);
  }

  /**
   * Accept the visitor.
   **/
  public Object jjtAccept(OrientSqlVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    left.toString(params, builder);
    if (leftModifier != null) {
      leftModifier.toString(params, builder);
    }
    switch (operator) {
    case OPERATOR_EQ:
      builder.append(" = ");
      break;
    case OPERATOR_PLUSASSIGN:
      builder.append(" += ");
      break;
    case OPERATOR_MINUSASSIGN:
      builder.append(" -= ");
      break;
    case OPERATOR_STARASSIGN:
      builder.append(" *= ");
      break;
    case OPERATOR_SLASHASSIGN:
      builder.append(" /= ");
      break;

    }
    right.toString(params, builder);
  }

  public OUpdateItem copy() {
    OUpdateItem result = new OUpdateItem(-1);
    result.left = left == null ? null : left.copy();
    result.leftModifier = leftModifier == null ? null : leftModifier.copy();
    result.operator = operator;
    result.right = right == null ? null : right.copy();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    OUpdateItem that = (OUpdateItem) o;

    if (operator != that.operator)
      return false;
    if (left != null ? !left.equals(that.left) : that.left != null)
      return false;
    if (leftModifier != null ? !leftModifier.equals(that.leftModifier) : that.leftModifier != null)
      return false;
    if (right != null ? !right.equals(that.right) : that.right != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = left != null ? left.hashCode() : 0;
    result = 31 * result + (leftModifier != null ? leftModifier.hashCode() : 0);
    result = 31 * result + operator;
    result = 31 * result + (right != null ? right.hashCode() : 0);
    return result;
  }

  public void applyUpdate(OResultInternal doc, OCommandContext ctx) {
    Object rightValue = right.execute(doc, ctx);
    if (leftModifier == null) {
      applyOperation(doc, left, rightValue, ctx);
    } else {
      Object val = doc.getProperty(left.getStringValue());
      leftModifier.setValue(doc, val, rightValue, ctx);
    }
  }

  public void applyOperation(OResultInternal doc, OIdentifier attrName, Object rightValue, OCommandContext ctx) {

    switch (operator) {
    case OPERATOR_EQ:
      Object newValue = convertResultToDocument(rightValue);
      newValue = convertToPropertyType(doc, attrName, newValue, ctx);
      doc.setProperty(attrName.getStringValue(), newValue);
      break;
    case OPERATOR_MINUSASSIGN:
      doc.setProperty(attrName.getStringValue(), calculateNewValue(doc, ctx, OMathExpression.Operator.MINUS));
      break;
    case OPERATOR_PLUSASSIGN:
      doc.setProperty(attrName.getStringValue(), calculateNewValue(doc, ctx, OMathExpression.Operator.PLUS));
      break;
    case OPERATOR_SLASHASSIGN:
      doc.setProperty(attrName.getStringValue(), calculateNewValue(doc, ctx, OMathExpression.Operator.SLASH));
      break;
    case OPERATOR_STARASSIGN:
      doc.setProperty(attrName.getStringValue(), calculateNewValue(doc, ctx, OMathExpression.Operator.STAR));
      break;
    }
  }

  public static Object convertToPropertyType(OResultInternal res, OIdentifier attrName, Object newValue, OCommandContext ctx) {
    OElement doc = res.toElement();
    Optional<OClass> optSchema = doc.getSchemaType();
    if (!optSchema.isPresent()) {
      return newValue;
    }
    OProperty prop = optSchema.get().getProperty(attrName.getStringValue());
    if (prop == null) {
      return newValue;
    }

    if (newValue instanceof Collection) {
      if (prop.getType() == OType.LINK) {
        if (((Collection) newValue).size() == 0) {
          newValue = null;
        } else if (((Collection) newValue).size() == 1) {
          newValue = ((Collection) newValue).iterator().next();
        } else {
          throw new OCommandExecutionException("Cannot assign a collection to a LINK property");
        }
      } else if (prop.getType() == OType.EMBEDDEDLIST && prop.getLinkedClass() != null) {
        return ((Collection) newValue).stream().map(item -> convertToType(item, prop.getLinkedClass(), ctx))
            .collect(Collectors.toList());

      } else if (prop.getType() == OType.EMBEDDEDSET && prop.getLinkedClass() != null) {
        return ((Collection) newValue).stream().map(item -> convertToType(item, prop.getLinkedClass(), ctx))
            .collect(Collectors.toSet());

      }
    }
    return newValue;
  }

  private static Object convertToType(Object item, OClass linkedClass, OCommandContext ctx) {
    if (item instanceof OElement) {
      OClass currentType = ((OElement) item).getSchemaType().orElse(null);
      if (currentType == null || !currentType.isSubClassOf(linkedClass)) {
        OElement result = ((ODatabaseSession) ctx.getDatabase()).newElement(linkedClass.getName());
        for (String prop : ((OElement) item).getPropertyNames()) {
          result.setProperty(prop, ((OElement) item).getProperty(prop));
        }
        return result;
      } else {
        return item;
      }
    } else if (item instanceof Map) {
      OElement result = ((ODatabaseSession) ctx.getDatabase()).newElement(linkedClass.getName());
      ((Map<String, Object>) item).entrySet().stream().forEach(x -> result.setProperty(x.getKey(), x.getValue()));
      return result;
    }
    return item;
  }

  private Object convertResultToDocument(Object value) {
    if (value instanceof OResult) {
      return ((OResult) value).toElement();
    }
    if (value instanceof OIdentifiable) {
      return value;
    }
    if (value instanceof List && containsOResult((Collection) value)) {
      return ((List) value).stream().map(x -> convertResultToDocument(x)).collect(Collectors.toList());
    }
    if (value instanceof Set && containsOResult((Collection) value)) {
      return ((Set) value).stream().map(x -> convertResultToDocument(x)).collect(Collectors.toSet());
    }
    return value;
  }

  private boolean containsOResult(Collection value) {
    return value.stream().anyMatch(x -> x instanceof OResult);
  }

  private Object calculateNewValue(OResultInternal doc, OCommandContext ctx, OMathExpression.Operator explicitOperator) {
    OExpression leftEx = new OExpression(left.copy());
    if (leftModifier != null) {
      ((OBaseExpression) leftEx.mathExpression).modifier = leftModifier.copy();
    }
    OMathExpression mathExp = new OMathExpression(-1);
    mathExp.getChildExpressions().add(leftEx.getMathExpression());
    mathExp.getChildExpressions().add(new OParenthesisExpression(right.copy()));
    mathExp.getOperators().add(explicitOperator);
    return mathExp.execute(doc, ctx);
  }

  public OIdentifier getLeft() {
    return left;
  }

  public void setLeft(OIdentifier left) {
    this.left = left;
  }

  public OModifier getLeftModifier() {
    return leftModifier;
  }

  public void setLeftModifier(OModifier leftModifier) {
    this.leftModifier = leftModifier;
  }

  public int getOperator() {
    return operator;
  }

  public void setOperator(int operator) {
    this.operator = operator;
  }

  public OExpression getRight() {
    return right;
  }

  public void setRight(OExpression right) {
    this.right = right;
  }
}
/* JavaCC - OriginalChecksum=df7444be87bba741316df8df0d653600 (do not edit this line) */
