package daomephsta.unpick.constantmappers.datadriven.tree;

import org.jetbrains.annotations.Nullable;

public abstract class ForwardingUnpickV3Visitor extends UnpickV3Visitor {
	@Nullable
	protected final UnpickV3Visitor downstream;

	public ForwardingUnpickV3Visitor(@Nullable UnpickV3Visitor downstream) {
		this.downstream = downstream;
	}

	@Override
	public void visitHeader(int version) {
		if (downstream != null) {
			downstream.visitHeader(version);
		}
	}

	@Override
	public void visitGroupDefinition(GroupDefinition groupDefinition) {
		if (downstream != null) {
			downstream.visitGroupDefinition(groupDefinition);
		}
	}

	@Override
	public void visitTargetField(TargetField targetField) {
		if (downstream != null) {
			downstream.visitTargetField(targetField);
		}
	}

	@Override
	public void visitTargetMethod(TargetMethod targetMethod) {
		if (downstream != null) {
			downstream.visitTargetMethod(targetMethod);
		}
	}

	@Override
	public void visitTargetAnnotation(TargetAnnotation targetAnnotation) {
		if (downstream != null) {
			downstream.visitTargetAnnotation(targetAnnotation);
		}
	}
}
