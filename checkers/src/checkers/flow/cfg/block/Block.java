package checkers.flow.cfg.block;

import java.util.Map;


/**
 * Represents a basic block in a control flow graph.
 * 
 * @author Stefan Heule
 * 
 */
public interface Block {
	
	/** The types of basic blocks */
	public static enum BlockType {

		/** A regular basic block. */
		REGULAR_BLOCK,

		/** A conditional basic block. */
		CONDITIONAL_BLOCK,

		/** A special basic block. */
		SPECIAL_BLOCK,
	}

	/**
	 * @return The type of this basic block.
	 */
	BlockType getType();

	/**
	 * @return The unique identifier of this node.
	 */
	long getId();
	
	/**
	 * @return The list of exceptional successor blocks.
	 */
	Map<Class<? extends Throwable>, Block> getExceptionalSuccessors();

}
