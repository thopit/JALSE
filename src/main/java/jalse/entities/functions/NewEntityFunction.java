package jalse.entities.functions;

import static jalse.entities.Entities.isEntitySubtype;
import static jalse.entities.functions.Functions.checkHasReturnType;
import static jalse.entities.functions.Functions.checkNotDefault;
import jalse.attributes.AttributeContainer;
import jalse.entities.DefaultEntityProxyFactory;
import jalse.entities.Entity;
import jalse.entities.EntityContainer;
import jalse.entities.annotations.EntityID;
import jalse.entities.annotations.NewEntity;
import jalse.entities.methods.NewEntityMethod;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * This is a method function for {@link NewEntity} annotation. It will resolve an
 * {@link NewEntityMethod} to be used by the entity typing system.<br>
 * <br>
 * The next example signatures will resolve to {@link EntityContainer#newEntity()}.
 *
 * <pre>
 * <code>
 * {@code @NewEntity}
 * Ghost newGhost();
 * 
 * {@code @EntityID(random = true)}
 * {@code @NewEntity}
 * Ghost newGhost();
 * </code>
 * </pre>
 *
 * The next example signatures will resolve to {@link EntityContainer#newEntity(java.util.UUID)}.
 *
 * <pre>
 * <code>
 * {@code @NewEntity}
 * Ghost newGhost(UUID id);
 * 
 * {@code @EntityID(mostSigBits = 0, leastSigBits = 1)}
 * {@code @NewEntity}
 * Ghost newGhost();
 * </code>
 * </pre>
 *
 * The next example signatures will resolve to
 * {@link EntityContainer#newEntity(java.util.UUID, AttributeContainer)}.
 *
 * <pre>
 * <code>
 * {@code @NewEntity}
 * Ghost newGhost(UUID id, AttributeContainer container);
 * 
 * {@code @EntityID(mostSigBits = 0, leastSigBits = 1)}
 * {@code @NewEntity}
 * Ghost newGhost(AttributeContainer container);
 * </code>
 * </pre>
 *
 * The next example signatures will resolve to {@link EntityContainer#newEntity(AttributeContainer)}
 * .
 *
 * <pre>
 * <code>
 * {@code @NewEntity} Ghost newGhost(AttributeContainer container);
 * 
 * {@code @EntityID(random = true)}
 * {@code @NewEntity}
 * Ghost newGhost(AttributeContainer container);
 * </code>
 * </pre>
 *
 * NOTE: This function will throw exceptions if {@link NewEntity} is present but the method
 * signature is invalid.
 *
 * @author Elliot Ford
 *
 * @see DefaultEntityProxyFactory
 *
 */
public class NewEntityFunction implements EntityMethodFunction {

    @SuppressWarnings("unchecked")
    @Override
    public NewEntityMethod apply(final Method m) {
	// Check for annotation
	final NewEntity annonation = m.getAnnotation(NewEntity.class);
	if (annonation == null) {
	    return null;
	}

	// Basic check method signature
	checkHasReturnType(m);
	checkNotDefault(m);
	if (m.getParameterCount() > 2) {
	    throw new IllegalArgumentException("Cannot have over two params");
	}

	// Check return type
	final Class<?> returnType = m.getReturnType();
	if (!Entity.class.equals(returnType) && !isEntitySubtype(returnType)) {
	    throw new IllegalArgumentException("Return type must be entity or type descendant");
	}

	// Get and validate ID
	final Supplier<UUID> idSupplier = Functions.getSingleIDSupplier(m);

	// Work out method type
	final Class<?>[] params = m.getParameterTypes();

	// Duplicate ID defintiion
	if (params.length >= 1 && UUID.class.equals(params[0]) && idSupplier != null) {
	    throw new IllegalArgumentException(String.format("Cannot have %s annotation and ID param", EntityID.class));
	}

	// If one param must be ID or container
	if (params.length == 1 && !UUID.class.equals(params[0]) && !AttributeContainer.class.equals(params[0])) {
	    throw new IllegalArgumentException("To have one param it must be ID or container");
	}

	// If two params must be ID and container
	if (params.length == 2 && (!UUID.class.equals(params[0]) || !AttributeContainer.class.equals(params[1]))) {
	    throw new IllegalArgumentException("To have two params they must be ID and container");
	}

	// Wether it is a container method
	final boolean idParam = params.length == 2 || params.length == 1 && UUID.class.equals(params[0]);
	final boolean containerParam = params.length == 2 || params.length == 1
		&& AttributeContainer.class.equals(params[0]);

	// Create new entity method
	final NewEntityMethod nem = new NewEntityMethod((Class<? extends Entity>) returnType);
	nem.setRequiresIDParam(idParam);
	nem.setRequiresContainerParam(containerParam);
	if (!idParam) {
	    nem.setIDSupplier(idSupplier);
	}
	return nem;
    }
}
