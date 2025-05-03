package daomephsta.unpick.impl.constantmappers.datadriven.data;

import daomephsta.unpick.constantmappers.datadriven.tree.expr.Expression;

public record ConstantReplacementInfo(boolean strict, Expression replacementExpression) {
}
