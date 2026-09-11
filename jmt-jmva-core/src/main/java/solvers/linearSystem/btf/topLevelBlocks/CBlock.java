package solvers.linearSystem.btf.topLevelBlocks;

import solvers.basis.CoMoMBasis;
import solvers.dataStructures.QNModel;
import solvers.exceptions.BTFMatrixErrorException;
import solvers.exceptions.InconsistentLinearSystemException;
import solvers.exceptions.InternalErrorException;
import solvers.linearSystem.btf.Position;
import solvers.linearSystem.btf.macroBlocks.CMacroBlock;
import solvers.linearSystem.btf.macroBlocks.MacroBlock;

/**
 * This class implements the Carry Forward Equations of the linear system
 * 
 * The Factory Methods newMacroBlock and subBlockCopy are implemented to return the correct 
 * block type in the parallel hierarchy.
 * 
 * Type 2a Macro Blocks are selected for lower class linear systems
 * 
 * @author Jack Bradshaw
 */
public class CBlock extends TopLevelBlock {

	public CBlock(QNModel qnm, CoMoMBasis basis) throws BTFMatrixErrorException, InternalErrorException, InconsistentLinearSystemException {
		super(qnm, basis, new Position(0,0));	
		selection_policy = new TypeTwoABlocks(qnm, this, current_class);
	}

	public CBlock(QNModel qnm, CoMoMBasis basis, Position position) throws BTFMatrixErrorException, InternalErrorException, InconsistentLinearSystemException {
		super(qnm, basis, position);		
		selection_policy = new TypeTwoABlocks(qnm, this, current_class);
	}

	public CBlock(CBlock full_block, int current_class) throws BTFMatrixErrorException {
		super(full_block,  current_class);				
	}
	
	@Override
	protected TopLevelBlock subBlockCopy(int current_class) throws BTFMatrixErrorException, InternalErrorException, InconsistentLinearSystemException {
		return new CBlock(this, current_class);
	}
	
	@Override
	protected MacroBlock newMacroBlock(Position block_position, int h) throws InternalErrorException, InconsistentLinearSystemException {
		return new CMacroBlock(qnm, basis, block_position, h);
	}
}
