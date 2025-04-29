package daomephsta.unpick.constantmappers.datadriven.parser.v2;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import daomephsta.unpick.constantmappers.datadriven.parser.MemberKey;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader.TargetMethodDefinitionVisitor;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader.Visitor;

/**
 * Remaps names and descriptors of target method definitions, then makes a delegate visitor visit the remapped target methods.
 * All other visitor methods only delegate to the delegate visitor.
 * @author Daomephsta
 */
public class UnpickV2Remapper implements Visitor {
	private static final Pattern OBJECT_SIGNATURE_FINDER = Pattern.compile("L([a-zA-Z0-9$_\\/]+);");
	private final Map<String, String> classMappings;
	private final Map<MemberKey, String> methodMappings;
	private final Map<MemberKey, String> fieldMappings;
	private final Visitor delegate;

	private UnpickV2Remapper(@SuppressWarnings("unused") Object disambiguator, Map<String, String> classMappings, Map<MemberKey, String> methodMappings, Map<MemberKey, String> fieldMappings, Visitor delegate) {
		this.classMappings = classMappings;
		this.methodMappings = methodMappings;
		this.fieldMappings = fieldMappings;
		this.delegate = delegate;
	}

	/**
	 * Creates a new {@link UnpickV2Remapper}.
	 * @param classMappings a mapping of old class names to new class names.
	 * @param methodMappings a mapping of old method names, owner classes, and descriptors; to new method names.
	 * @param fieldMappings a mapping of old field names and owner classes, and descriptors; to new field names.
	 * @param delegate the visitor that should visit the remapped target method definitions.
	 * All other visitor methods only delegate to the delegate visitor.
	 */
	public static UnpickV2Remapper create(Map<String, String> classMappings, Map<MemberKey, String> methodMappings, Map<MemberKey, String> fieldMappings, Visitor delegate) {
		return new UnpickV2Remapper(null, classMappings, methodMappings, fieldMappings, delegate);
	}

	private String remapClass(String name) {
		return classMappings.getOrDefault(name, name);
	}

	private String remapMethod(String owner, String name, String descriptor) {
		return methodMappings.getOrDefault(new MemberKey(owner, name, descriptor), name);
	}

	private String remapField(String owner, String name, String descriptor) {
		return fieldMappings.getOrDefault(new MemberKey(owner, name, descriptor), name);
	}

	private String remapDescriptor(String descriptor) {
		String remappedDescriptor = descriptor;

		if (descriptor != null) {
			Matcher objectSignatureMatcher = OBJECT_SIGNATURE_FINDER.matcher(descriptor);
			while (objectSignatureMatcher.find()) {
				String objectSignature = objectSignatureMatcher.group(1);
				if (classMappings.containsKey(objectSignature)) {
					remappedDescriptor = remappedDescriptor.replace(objectSignature, classMappings.get(objectSignature));
				}
			}
		}

		return remappedDescriptor;
	}

	public TargetMethodDefinitionVisitor visitTargetMethodDefinition(String owner, String name, String descriptor) {
		//Reassigning the parameters tends to cause bugs
		String remappedOwner = remapClass(owner);
		String remappedName = remapMethod(owner, name, descriptor);
		String remappedDescriptor = remapDescriptor(descriptor);

		return delegate.visitTargetMethodDefinition(remappedOwner, remappedName, remappedDescriptor);
	}

	public void startVisit() {
		delegate.startVisit();
	}

	public void visitLineNumber(int lineNumber) {
		delegate.visitLineNumber(lineNumber);
	}

	public void visitSimpleConstantDefinition(String group, String owner, String name, String value, String descriptor) {
		//Reassigning the parameters tends to cause bugs
		String remappedOwner = remapClass(owner);
		String remappedName = remapField(owner, name, descriptor);
		String remappedDescriptor = remapDescriptor(descriptor);

		delegate.visitSimpleConstantDefinition(group, remappedOwner, remappedName, value, remappedDescriptor);
	}

	public void visitFlagConstantDefinition(String group, String owner, String name, String value, String descriptor) {
		//Reassigning the parameters tends to cause bugs
		String remappedOwner = remapClass(owner);
		String remappedName = remapField(owner, name, descriptor);
		String remappedDescriptor = remapDescriptor(descriptor);

		delegate.visitFlagConstantDefinition(group, remappedOwner, remappedName, value, remappedDescriptor);
	}

	public void endVisit() {
		delegate.endVisit();
	}
}
