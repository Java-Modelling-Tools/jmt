package solvers.linearSystem.btf.topLevelBlocks;

import solvers.basis.CoMoMBasis;
import solvers.dataStructures.QNModel;
import solvers.exceptions.BTFMatrixErrorException;
import solvers.exceptions.InconsistentLinearSystemException;
import solvers.exceptions.InternalErrorException;
import solvers.linearSystem.btf.Position;
import solvers.linearSystem.btf.macroBlocks.B1MacroBlock;
import solvers.linearSystem.btf.macroBlocks.MacroBlock;

/**
 * This class implements the B1 Block of the B matrix.
 * 
 * The Factory Methods newMacroBlock and subBlockCopy are implemented to return the correct 
 * block type in the parallel hierarchy.
 * 
 * Type 1 Macro Blocks are selected for lower class linear systems
 * 
 * @author Jack Bradshaw
 */
public class B1Block extends TopLevelBlock {

	public B1Block(QNModel qnm, CoMoMBasis basis) //TODO why two?
			throws BTFMatrixErrorException, InternalErrorException, InconsistentLinearSystemException {
		super(qnm, basis, new Position(0,0));	
		selection_policy = new TypeOneBlocks(qnm, this, current_class);
	}
	
	public B1Block(QNModel qnm, CoMoMBasis basis, Position position)
			throws BTFMatrixErrorException, InternalErrorException, InconsistentLinearSystemException {
		super(qnm, basis, position);	
		selection_policy = new TypeOneBlocks(qnm, this, current_class);
	}

	public B1Block(B1Block full_block, int current_class) {
		super(full_block, current_class);
	}
	
	@Override
	protected TopLevelBlock subBlockCopy(int current_class) throws BTFMatrixErrorException, InternalErrorException, InconsistentLinearSystemException {
		return new B1Block(this, current_class);
	}

	@Override
	protected MacroBlock newMacroBlock(Position block_position, int h) throws InternalErrorException, InconsistentLinearSystemException {
		return new B1MacroBlock(qnm, basis, block_position, h);
	}
}
