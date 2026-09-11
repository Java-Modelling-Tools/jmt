package solvers.linearSystem.btf.macroBlocks;

import solvers.basis.CoMoMBasis;
import solvers.dataStructures.QNModel;
import solvers.exceptions.InconsistentLinearSystemException;
import solvers.exceptions.InternalErrorException;
import solvers.linearSystem.btf.Position;
import solvers.linearSystem.btf.microBlocks.MicroBlock;
import solvers.linearSystem.btf.microBlocks.YMicroBlock;

/**
 * @author Jack Bradshaw
 */
public class YMacroBlock extends MacroBlock {

	public YMacroBlock(QNModel qnm, CoMoMBasis basis, Position position, int h) throws InternalErrorException, InconsistentLinearSystemException {
		super(qnm, basis, position, h);
		selection_policy = new TypeOneBlocks(qnm, this); 
	}

	public YMacroBlock(MacroBlock full_block, int current_class) {
		super(full_block, current_class);
	}

	@Override
	protected MacroBlock subBlockCopy(int current_class) {
		return new YMacroBlock(this, current_class);
	}
	
	@Override
	protected MicroBlock newMicroBlock(Position block_position, int h) throws InternalErrorException {
		return new YMicroBlock(qnm, basis, block_position, h);
	}
}
