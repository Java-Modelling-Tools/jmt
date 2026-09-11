package solvers.linearSystem.btf.macroBlocks;

import solvers.basis.CoMoMBasis;
import solvers.dataStructures.QNModel;
import solvers.exceptions.InconsistentLinearSystemException;
import solvers.exceptions.InternalErrorException;
import solvers.linearSystem.btf.Position;
import solvers.linearSystem.btf.microBlocks.MicroBlock;
import solvers.linearSystem.btf.microBlocks.XMicroBlock;

/**
 * This class implements the Macro Blocks of the X Block of the A matrix.
 * 
 * The Factory Methods newMicroBlock and subBlockCopy are implemented to return the correct 
 * block type in the parallel hierarchy.
 * 
 * Type 1 Micro Blocks are selected for lower class linear systems
 * 
 * @author Jack Bradshaw
 */
public class XMacroBlock extends MacroBlock {

	public XMacroBlock(QNModel qnm, CoMoMBasis basis, Position position, int h) throws InternalErrorException, InconsistentLinearSystemException {
		super(qnm, basis, position, h);
		selection_policy = new TypeOneBlocks(qnm, this); 
	}

	public XMacroBlock(MacroBlock full_block, int current_class) {
		super(full_block, current_class);
	}

	@Override
	protected MacroBlock subBlockCopy(int current_class) {
		return new XMacroBlock(this, current_class);
	}
	
	public void LUPDecompose(boolean in_place) throws InconsistentLinearSystemException {
		for (int i = 0; i < micro_blocks.length; i++) {
			((XMicroBlock) micro_blocks[i]).LUPDecompose(in_place);
		}
	}
	
	@Override
	protected MicroBlock newMicroBlock(Position block_position, int h) throws InconsistentLinearSystemException, InternalErrorException {
		return new XMicroBlock(qnm, basis, block_position, h);
	}
}
