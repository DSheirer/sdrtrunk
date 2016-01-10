package module.decode.p25;

import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.TapGroup;
import instrument.tap.stream.AdditiveFloatTap;
import instrument.tap.stream.FloatTap;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.real.RealBuffer;
import sample.real.RealSampleListener;
import sample.real.RealSampleProvider;
import source.tuner.frequency.FrequencyChangeEvent;
import source.tuner.frequency.FrequencyChangeEvent.Event;
import source.tuner.frequency.IFrequencyChangeListener;
import dsp.gain.DirectGainControl;

public class C4FMSymbolFilter implements Listener<RealBuffer>, 
										 IFrequencyChangeListener,
										 RealSampleProvider,
										 Instrumentable
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( C4FMSymbolFilter.class );
	
	private static final float TAPS[][] = 
	{ 
		{  0.00000e+00f,  0.00000e+00f,  0.00000e+00f,  0.00000e+00f,  1.00000e+00f,  0.00000e+00f,  0.00000e+00f,  0.00000e+00f }, //   0/128
		{ -1.54700e-04f,  8.53777e-04f, -2.76968e-03f,  7.89295e-03f,  9.98534e-01f, -5.41054e-03f,  1.24642e-03f, -1.98993e-04f }, //   1/128
		{ -3.09412e-04f,  1.70888e-03f, -5.55134e-03f,  1.58840e-02f,  9.96891e-01f, -1.07209e-02f,  2.47942e-03f, -3.96391e-04f }, //   2/128
		{ -4.64053e-04f,  2.56486e-03f, -8.34364e-03f,  2.39714e-02f,  9.95074e-01f, -1.59305e-02f,  3.69852e-03f, -5.92100e-04f }, //   3/128
		{ -6.18544e-04f,  3.42130e-03f, -1.11453e-02f,  3.21531e-02f,  9.93082e-01f, -2.10389e-02f,  4.90322e-03f, -7.86031e-04f }, //   4/128
		{ -7.72802e-04f,  4.27773e-03f, -1.39548e-02f,  4.04274e-02f,  9.90917e-01f, -2.60456e-02f,  6.09305e-03f, -9.78093e-04f }, //   5/128
		{ -9.26747e-04f,  5.13372e-03f, -1.67710e-02f,  4.87921e-02f, 9.88580e-01f, -3.09503e-02f,  7.26755e-03f, -1.16820e-03f }, //   6/128
		{ -1.08030e-03f,  5.98883e-03f, -1.95925e-02f,  5.72454e-02f,  9.86071e-01f, -3.57525e-02f,  8.42626e-03f, -1.35627e-03f }, //   7/128
		{ -1.23337e-03f,  6.84261e-03f, -2.24178e-02f,  6.57852e-02f,  9.83392e-01f, -4.04519e-02f,  9.56876e-03f, -1.54221e-03f }, //   8/128
		{ -1.38589e-03f,  7.69462e-03f, -2.52457e-02f,  7.44095e-02f,  9.80543e-01f, -4.50483e-02f,  1.06946e-02f, -1.72594e-03f }, //   9/128
		{ -1.53777e-03f,  8.54441e-03f, -2.80746e-02f,  8.31162e-02f,  9.77526e-01f, -4.95412e-02f,  1.18034e-02f, -1.90738e-03f }, //  10/128
		{ -1.68894e-03f,  9.39154e-03f, -3.09033e-02f,  9.19033e-02f,  9.74342e-01f, -5.39305e-02f,  1.28947e-02f, -2.08645e-03f }, //  11/128
		{ -1.83931e-03f,  1.02356e-02f, -3.37303e-02f,  1.00769e-01f,  9.70992e-01f, -5.82159e-02f,  1.39681e-02f, -2.26307e-03f }, //  12/128
		{ -1.98880e-03f,  1.10760e-02f, -3.65541e-02f,  1.09710e-01f,  9.67477e-01f, -6.23972e-02f,  1.50233e-02f, -2.43718e-03f }, //  13/128
		{ -2.13733e-03f,  1.19125e-02f, -3.93735e-02f,  1.18725e-01f,  9.63798e-01f, -6.64743e-02f,  1.60599e-02f, -2.60868e-03f }, //  14/128
		{ -2.28483e-03f,  1.27445e-02f, -4.21869e-02f,  1.27812e-01f,  9.59958e-01f, -7.04471e-02f,  1.70776e-02f, -2.77751e-03f }, //  15/128
		{ -2.43121e-03f,  1.35716e-02f, -4.49929e-02f,  1.36968e-01f,  9.55956e-01f, -7.43154e-02f,  1.80759e-02f, -2.94361e-03f }, //  16/128
		{ -2.57640e-03f,  1.43934e-02f, -4.77900e-02f,  1.46192e-01f,  9.51795e-01f, -7.80792e-02f,  1.90545e-02f, -3.10689e-03f }, //  17/128
		{ -2.72032e-03f,  1.52095e-02f, -5.05770e-02f,  1.55480e-01f,  9.47477e-01f, -8.17385e-02f,  2.00132e-02f, -3.26730e-03f }, //  18/128
		{ -2.86289e-03f,  1.60193e-02f, -5.33522e-02f,  1.64831e-01f,  9.43001e-01f, -8.52933e-02f,  2.09516e-02f, -3.42477e-03f }, //  19/128
		{ -3.00403e-03f,  1.68225e-02f, -5.61142e-02f,  1.74242e-01f,  9.38371e-01f, -8.87435e-02f,  2.18695e-02f, -3.57923e-03f }, //  20/128
		{ -3.14367e-03f,  1.76185e-02f, -5.88617e-02f,  1.83711e-01f,  9.33586e-01f, -9.20893e-02f,  2.27664e-02f, -3.73062e-03f }, //  21/128
		{ -3.28174e-03f,  1.84071e-02f, -6.15931e-02f,  1.93236e-01f,  9.28650e-01f, -9.53307e-02f,  2.36423e-02f, -3.87888e-03f }, //  22/128
		{ -3.41815e-03f,  1.91877e-02f, -6.43069e-02f,  2.02814e-01f,  9.23564e-01f, -9.84679e-02f,  2.44967e-02f, -4.02397e-03f }, //  23/128
		{ -3.55283e-03f,  1.99599e-02f, -6.70018e-02f,  2.12443e-01f,  9.18329e-01f, -1.01501e-01f,  2.53295e-02f, -4.16581e-03f }, //  24/128
		{ -3.68570e-03f,  2.07233e-02f, -6.96762e-02f,  2.22120e-01f,  9.12947e-01f, -1.04430e-01f,  2.61404e-02f, -4.30435e-03f }, //  25/128
		{ -3.81671e-03f,  2.14774e-02f, -7.23286e-02f,  2.31843e-01f,  9.07420e-01f, -1.07256e-01f,  2.69293e-02f, -4.43955e-03f }, //  26/128
		{ -3.94576e-03f,  2.22218e-02f, -7.49577e-02f,  2.41609e-01f,  9.01749e-01f, -1.09978e-01f,  2.76957e-02f, -4.57135e-03f }, //  27/128
		{ -4.07279e-03f,  2.29562e-02f, -7.75620e-02f,  2.51417e-01f,  8.95936e-01f, -1.12597e-01f,  2.84397e-02f, -4.69970e-03f }, //  28/128
		{ -4.19774e-03f,  2.36801e-02f, -8.01399e-02f,  2.61263e-01f,  8.89984e-01f, -1.15113e-01f,  2.91609e-02f, -4.82456e-03f }, //  29/128
		{ -4.32052e-03f,  2.43930e-02f, -8.26900e-02f,  2.71144e-01f,  8.83893e-01f, -1.17526e-01f,  2.98593e-02f, -4.94589e-03f }, //  30/128
		{ -4.44107e-03f,  2.50946e-02f, -8.52109e-02f,  2.81060e-01f,  8.77666e-01f, -1.19837e-01f,  3.05345e-02f, -5.06363e-03f }, //  31/128
		{ -4.55932e-03f,  2.57844e-02f, -8.77011e-02f,  2.91006e-01f,  8.71305e-01f, -1.22047e-01f,  3.11866e-02f, -5.17776e-03f }, //  32/128
		{ -4.67520e-03f,  2.64621e-02f, -9.01591e-02f,  3.00980e-01f,  8.64812e-01f, -1.24154e-01f,  3.18153e-02f, -5.28823e-03f }, //  33/128
		{ -4.78866e-03f,  2.71272e-02f, -9.25834e-02f,  3.10980e-01f,  8.58189e-01f, -1.26161e-01f,  3.24205e-02f, -5.39500e-03f }, //  34/128
		{ -4.89961e-03f,  2.77794e-02f, -9.49727e-02f,  3.21004e-01f,  8.51437e-01f, -1.28068e-01f,  3.30021e-02f, -5.49804e-03f }, //  35/128
		{ -5.00800e-03f,  2.84182e-02f, -9.73254e-02f,  3.31048e-01f,  8.44559e-01f, -1.29874e-01f,  3.35600e-02f, -5.59731e-03f }, //  36/128
		{ -5.11376e-03f,  2.90433e-02f, -9.96402e-02f,  3.41109e-01f,  8.37557e-01f, -1.31581e-01f,  3.40940e-02f, -5.69280e-03f }, //  37/128
		{ -5.21683e-03f,  2.96543e-02f, -1.01915e-01f,  3.51186e-01f,  8.30432e-01f, -1.33189e-01f,  3.46042e-02f, -5.78446e-03f }, //  38/128
		{ -5.31716e-03f,  3.02507e-02f, -1.04150e-01f,  3.61276e-01f,  8.23188e-01f, -1.34699e-01f,  3.50903e-02f, -5.87227e-03f }, //  39/128
		{ -5.41467e-03f,  3.08323e-02f, -1.06342e-01f,  3.71376e-01f,  8.15826e-01f, -1.36111e-01f,  3.55525e-02f, -5.95620e-03f }, //  40/128
		{ -5.50931e-03f,  3.13987e-02f, -1.08490e-01f,  3.81484e-01f,  8.08348e-01f, -1.37426e-01f,  3.59905e-02f, -6.03624e-03f }, //  41/128
		{ -5.60103e-03f,  3.19495e-02f, -1.10593e-01f,  3.91596e-01f,  8.00757e-01f, -1.38644e-01f,  3.64044e-02f, -6.11236e-03f }, //  42/128
		{ -5.68976e-03f,  3.24843e-02f, -1.12650e-01f,  4.01710e-01f,  7.93055e-01f, -1.39767e-01f,  3.67941e-02f, -6.18454e-03f }, //  43/128
		{ -5.77544e-03f,  3.30027e-02f, -1.14659e-01f,  4.11823e-01f,  7.85244e-01f, -1.40794e-01f,  3.71596e-02f, -6.25277e-03f }, //  44/128
		{ -5.85804e-03f,  3.35046e-02f, -1.16618e-01f,  4.21934e-01f,  7.77327e-01f, -1.41727e-01f,  3.75010e-02f, -6.31703e-03f }, //  45/128
		{ -5.93749e-03f,  3.39894e-02f, -1.18526e-01f,  4.32038e-01f,  7.69305e-01f, -1.42566e-01f,  3.78182e-02f, -6.37730e-03f }, //  46/128
		{ -6.01374e-03f,  3.44568e-02f, -1.20382e-01f,  4.42134e-01f,  7.61181e-01f, -1.43313e-01f,  3.81111e-02f, -6.43358e-03f }, //  47/128
		{ -6.08674e-03f,  3.49066e-02f, -1.22185e-01f,  4.52218e-01f,  7.52958e-01f, -1.43968e-01f,  3.83800e-02f, -6.48585e-03f }, //  48/128
		{ -6.15644e-03f,  3.53384e-02f, -1.23933e-01f,  4.62289e-01f,  7.44637e-01f, -1.44531e-01f,  3.86247e-02f, -6.53412e-03f }, //  49/128
		{ -6.22280e-03f,  3.57519e-02f, -1.25624e-01f,  4.72342e-01f,  7.36222e-01f, -1.45004e-01f,  3.88454e-02f, -6.57836e-03f }, //  50/128
		{ -6.28577e-03f,  3.61468e-02f, -1.27258e-01f,  4.82377e-01f,  7.27714e-01f, -1.45387e-01f,  3.90420e-02f, -6.61859e-03f }, //  51/128
		{ -6.34530e-03f,  3.65227e-02f, -1.28832e-01f,  4.92389e-01f,  7.19116e-01f, -1.45682e-01f,  3.92147e-02f, -6.65479e-03f }, //  52/128
		{ -6.40135e-03f,  3.68795e-02f, -1.30347e-01f,  5.02377e-01f,  7.10431e-01f, -1.45889e-01f,  3.93636e-02f, -6.68698e-03f }, //  53/128
		{ -6.45388e-03f,  3.72167e-02f, -1.31800e-01f,  5.12337e-01f,  7.01661e-01f, -1.46009e-01f,  3.94886e-02f, -6.71514e-03f }, //  54/128
		{ -6.50285e-03f,  3.75341e-02f, -1.33190e-01f,  5.22267e-01f,  6.92808e-01f, -1.46043e-01f,  3.95900e-02f, -6.73929e-03f }, //  55/128
		{ -6.54823e-03f,  3.78315e-02f, -1.34515e-01f,  5.32164e-01f,  6.83875e-01f, -1.45993e-01f,  3.96678e-02f, -6.75943e-03f }, //  56/128
		{ -6.58996e-03f,  3.81085e-02f, -1.35775e-01f,  5.42025e-01f,  6.74865e-01f, -1.45859e-01f,  3.97222e-02f, -6.77557e-03f }, //  57/128
		{ -6.62802e-03f,  3.83650e-02f, -1.36969e-01f,  5.51849e-01f,  6.65779e-01f, -1.45641e-01f,  3.97532e-02f, -6.78771e-03f }, //  58/128
		{ -6.66238e-03f,  3.86006e-02f, -1.38094e-01f,  5.61631e-01f,  6.56621e-01f, -1.45343e-01f,  3.97610e-02f, -6.79588e-03f }, //  59/128
		{ -6.69300e-03f,  3.88151e-02f, -1.39150e-01f,  5.71370e-01f,  6.47394e-01f, -1.44963e-01f,  3.97458e-02f, -6.80007e-03f }, //  60/128
		{ -6.71985e-03f,  3.90083e-02f, -1.40136e-01f,  5.81063e-01f,  6.38099e-01f, -1.44503e-01f,  3.97077e-02f, -6.80032e-03f }, //  61/128
		{ -6.74291e-03f,  3.91800e-02f, -1.41050e-01f,  5.90706e-01f,  6.28739e-01f, -1.43965e-01f,  3.96469e-02f, -6.79662e-03f }, //  62/128
		{ -6.76214e-03f,  3.93299e-02f, -1.41891e-01f,  6.00298e-01f,  6.19318e-01f, -1.43350e-01f,  3.95635e-02f, -6.78902e-03f }, //  63/128
		{ -6.77751e-03f,  3.94578e-02f, -1.42658e-01f,  6.09836e-01f,  6.09836e-01f, -1.42658e-01f,  3.94578e-02f, -6.77751e-03f }, //  64/128
		{ -6.78902e-03f,  3.95635e-02f, -1.43350e-01f,  6.19318e-01f,  6.00298e-01f, -1.41891e-01f,  3.93299e-02f, -6.76214e-03f }, //  65/128
		{ -6.79662e-03f,  3.96469e-02f, -1.43965e-01f,  6.28739e-01f,  5.90706e-01f, -1.41050e-01f,  3.91800e-02f, -6.74291e-03f }, //  66/128
		{ -6.80032e-03f,  3.97077e-02f, -1.44503e-01f,  6.38099e-01f,  5.81063e-01f, -1.40136e-01f,  3.90083e-02f, -6.71985e-03f }, //  67/128
		{ -6.80007e-03f,  3.97458e-02f, -1.44963e-01f,  6.47394e-01f,  5.71370e-01f, -1.39150e-01f,  3.88151e-02f, -6.69300e-03f }, //  68/128
		{ -6.79588e-03f,  3.97610e-02f, -1.45343e-01f,  6.56621e-01f,  5.61631e-01f, -1.38094e-01f,  3.86006e-02f, -6.66238e-03f }, //  69/128
		{ -6.78771e-03f,  3.97532e-02f, -1.45641e-01f,  6.65779e-01f,  5.51849e-01f, -1.36969e-01f,  3.83650e-02f, -6.62802e-03f }, //  70/128
		{ -6.77557e-03f,  3.97222e-02f, -1.45859e-01f,  6.74865e-01f,  5.42025e-01f, -1.35775e-01f,  3.81085e-02f, -6.58996e-03f }, //  71/128
		{ -6.75943e-03f,  3.96678e-02f, -1.45993e-01f,  6.83875e-01f,  5.32164e-01f, -1.34515e-01f,  3.78315e-02f, -6.54823e-03f }, //  72/128
		{ -6.73929e-03f,  3.95900e-02f, -1.46043e-01f,  6.92808e-01f,  5.22267e-01f, -1.33190e-01f,  3.75341e-02f, -6.50285e-03f }, //  73/128
		{ -6.71514e-03f,  3.94886e-02f, -1.46009e-01f,  7.01661e-01f,  5.12337e-01f, -1.31800e-01f,  3.72167e-02f, -6.45388e-03f }, //  74/128
		{ -6.68698e-03f,  3.93636e-02f, -1.45889e-01f,  7.10431e-01f,  5.02377e-01f, -1.30347e-01f,  3.68795e-02f, -6.40135e-03f }, //  75/128
		{ -6.65479e-03f,  3.92147e-02f, -1.45682e-01f,  7.19116e-01f,  4.92389e-01f, -1.28832e-01f,  3.65227e-02f, -6.34530e-03f }, //  76/128
		{ -6.61859e-03f,  3.90420e-02f, -1.45387e-01f,  7.27714e-01f,  4.82377e-01f, -1.27258e-01f,  3.61468e-02f, -6.28577e-03f }, //  77/128
		{ -6.57836e-03f,  3.88454e-02f, -1.45004e-01f,  7.36222e-01f,  4.72342e-01f, -1.25624e-01f,  3.57519e-02f, -6.22280e-03f }, //  78/128
		{ -6.53412e-03f,  3.86247e-02f, -1.44531e-01f,  7.44637e-01f,  4.62289e-01f, -1.23933e-01f,  3.53384e-02f, -6.15644e-03f }, //  79/128
		{ -6.48585e-03f,  3.83800e-02f, -1.43968e-01f,  7.52958e-01f,  4.52218e-01f, -1.22185e-01f,  3.49066e-02f, -6.08674e-03f }, //  80/128
		{ -6.43358e-03f,  3.81111e-02f, -1.43313e-01f,  7.61181e-01f,  4.42134e-01f, -1.20382e-01f,  3.44568e-02f, -6.01374e-03f }, //  81/128
		{ -6.37730e-03f,  3.78182e-02f, -1.42566e-01f,  7.69305e-01f,  4.32038e-01f, -1.18526e-01f,  3.39894e-02f, -5.93749e-03f }, //  82/128
		{ -6.31703e-03f,  3.75010e-02f, -1.41727e-01f,  7.77327e-01f,  4.21934e-01f, -1.16618e-01f,  3.35046e-02f, -5.85804e-03f }, //  83/128
		{ -6.25277e-03f,  3.71596e-02f, -1.40794e-01f,  7.85244e-01f,  4.11823e-01f, -1.14659e-01f,  3.30027e-02f, -5.77544e-03f }, //  84/128
		{ -6.18454e-03f,  3.67941e-02f, -1.39767e-01f,  7.93055e-01f,  4.01710e-01f, -1.12650e-01f,  3.24843e-02f, -5.68976e-03f }, //  85/128
		{ -6.11236e-03f,  3.64044e-02f, -1.38644e-01f,  8.00757e-01f,  3.91596e-01f, -1.10593e-01f,  3.19495e-02f, -5.60103e-03f }, //  86/128
		{ -6.03624e-03f,  3.59905e-02f, -1.37426e-01f,  8.08348e-01f,  3.81484e-01f, -1.08490e-01f,  3.13987e-02f, -5.50931e-03f }, //  87/128
		{ -5.95620e-03f,  3.55525e-02f, -1.36111e-01f,  8.15826e-01f,  3.71376e-01f, -1.06342e-01f,  3.08323e-02f, -5.41467e-03f }, //  88/128
		{ -5.87227e-03f,  3.50903e-02f, -1.34699e-01f,  8.23188e-01f,  3.61276e-01f, -1.04150e-01f,  3.02507e-02f, -5.31716e-03f }, //  89/128
		{ -5.78446e-03f,  3.46042e-02f, -1.33189e-01f,  8.30432e-01f,  3.51186e-01f, -1.01915e-01f,  2.96543e-02f, -5.21683e-03f }, //  90/128
		{ -5.69280e-03f,  3.40940e-02f, -1.31581e-01f,  8.37557e-01f,  3.41109e-01f, -9.96402e-02f,  2.90433e-02f, -5.11376e-03f }, //  91/128
		{ -5.59731e-03f,  3.35600e-02f, -1.29874e-01f,  8.44559e-01f,  3.31048e-01f, -9.73254e-02f,  2.84182e-02f, -5.00800e-03f }, //  92/128
		{ -5.49804e-03f,  3.30021e-02f, -1.28068e-01f,  8.51437e-01f,  3.21004e-01f, -9.49727e-02f,  2.77794e-02f, -4.89961e-03f }, //  93/128
		{ -5.39500e-03f,  3.24205e-02f, -1.26161e-01f,  8.58189e-01f,  3.10980e-01f, -9.25834e-02f,  2.71272e-02f, -4.78866e-03f }, //  94/128
		{ -5.28823e-03f,  3.18153e-02f, -1.24154e-01f,  8.64812e-01f,  3.00980e-01f, -9.01591e-02f,  2.64621e-02f, -4.67520e-03f }, //  95/128
		{ -5.17776e-03f,  3.11866e-02f, -1.22047e-01f,  8.71305e-01f,  2.91006e-01f, -8.77011e-02f,  2.57844e-02f, -4.55932e-03f }, //  96/128
		{ -5.06363e-03f,  3.05345e-02f, -1.19837e-01f,  8.77666e-01f,  2.81060e-01f, -8.52109e-02f,  2.50946e-02f, -4.44107e-03f }, //  97/128
		{ -4.94589e-03f,  2.98593e-02f, -1.17526e-01f,  8.83893e-01f,  2.71144e-01f, -8.26900e-02f,  2.43930e-02f, -4.32052e-03f }, //  98/128
		{ -4.82456e-03f,  2.91609e-02f, -1.15113e-01f,  8.89984e-01f,  2.61263e-01f, -8.01399e-02f,  2.36801e-02f, -4.19774e-03f }, //  99/128
		{ -4.69970e-03f,  2.84397e-02f, -1.12597e-01f,  8.95936e-01f,  2.51417e-01f, -7.75620e-02f,  2.29562e-02f, -4.07279e-03f }, // 100/128
		{ -4.57135e-03f,  2.76957e-02f, -1.09978e-01f,  9.01749e-01f,  2.41609e-01f, -7.49577e-02f,  2.22218e-02f, -3.94576e-03f }, // 101/128
		{ -4.43955e-03f,  2.69293e-02f, -1.07256e-01f,  9.07420e-01f,  2.31843e-01f, -7.23286e-02f,  2.14774e-02f, -3.81671e-03f }, // 102/128
		{ -4.30435e-03f,  2.61404e-02f, -1.04430e-01f,  9.12947e-01f,  2.22120e-01f, -6.96762e-02f,  2.07233e-02f, -3.68570e-03f }, // 103/128
		{ -4.16581e-03f,  2.53295e-02f, -1.01501e-01f,  9.18329e-01f,  2.12443e-01f, -6.70018e-02f,  1.99599e-02f, -3.55283e-03f }, // 104/128
		{ -4.02397e-03f,  2.44967e-02f, -9.84679e-02f,  9.23564e-01f,  2.02814e-01f, -6.43069e-02f,  1.91877e-02f, -3.41815e-03f }, // 105/128
		{ -3.87888e-03f,  2.36423e-02f, -9.53307e-02f,  9.28650e-01f,  1.93236e-01f, -6.15931e-02f,  1.84071e-02f, -3.28174e-03f }, // 106/128
		{ -3.73062e-03f,  2.27664e-02f, -9.20893e-02f,  9.33586e-01f,  1.83711e-01f, -5.88617e-02f,  1.76185e-02f, -3.14367e-03f }, // 107/128
		{ -3.57923e-03f,  2.18695e-02f, -8.87435e-02f,  9.38371e-01f,  1.74242e-01f, -5.61142e-02f,  1.68225e-02f, -3.00403e-03f }, // 108/128
		{ -3.42477e-03f,  2.09516e-02f, -8.52933e-02f,  9.43001e-01f,  1.64831e-01f, -5.33522e-02f,  1.60193e-02f, -2.86289e-03f }, // 109/128
		{ -3.26730e-03f,  2.00132e-02f, -8.17385e-02f,  9.47477e-01f,  1.55480e-01f, -5.05770e-02f,  1.52095e-02f, -2.72032e-03f }, // 110/128
		{ -3.10689e-03f,  1.90545e-02f, -7.80792e-02f,  9.51795e-01f,  1.46192e-01f, -4.77900e-02f,  1.43934e-02f, -2.57640e-03f }, // 111/128
		{ -2.94361e-03f,  1.80759e-02f, -7.43154e-02f,  9.55956e-01f,  1.36968e-01f, -4.49929e-02f,  1.35716e-02f, -2.43121e-03f }, // 112/128
		{ -2.77751e-03f,  1.70776e-02f, -7.04471e-02f,  9.59958e-01f,  1.27812e-01f, -4.21869e-02f,  1.27445e-02f, -2.28483e-03f }, // 113/128
		{ -2.60868e-03f,  1.60599e-02f, -6.64743e-02f,  9.63798e-01f,  1.18725e-01f, -3.93735e-02f,  1.19125e-02f, -2.13733e-03f }, // 114/128
		{ -2.43718e-03f,  1.50233e-02f, -6.23972e-02f,  9.67477e-01f,  1.09710e-01f, -3.65541e-02f,  1.10760e-02f, -1.98880e-03f }, // 115/128
		{ -2.26307e-03f,  1.39681e-02f, -5.82159e-02f,  9.70992e-01f,  1.00769e-01f, -3.37303e-02f,  1.02356e-02f, -1.83931e-03f }, // 116/128
		{ -2.08645e-03f,  1.28947e-02f, -5.39305e-02f,  9.74342e-01f,  9.19033e-02f, -3.09033e-02f,  9.39154e-03f, -1.68894e-03f }, // 117/128
		{ -1.90738e-03f,  1.18034e-02f, -4.95412e-02f,  9.77526e-01f,  8.31162e-02f, -2.80746e-02f,  8.54441e-03f, -1.53777e-03f }, // 118/128
		{ -1.72594e-03f,  1.06946e-02f, -4.50483e-02f,  9.80543e-01f,  7.44095e-02f, -2.52457e-02f,  7.69462e-03f, -1.38589e-03f }, // 119/128
		{ -1.54221e-03f,  9.56876e-03f, -4.04519e-02f,  9.83392e-01f,  6.57852e-02f, -2.24178e-02f,  6.84261e-03f, -1.23337e-03f }, // 120/128
		{ -1.35627e-03f,  8.42626e-03f, -3.57525e-02f,  9.86071e-01f,  5.72454e-02f, -1.95925e-02f,  5.98883e-03f, -1.08030e-03f }, // 121/128
		{ -1.16820e-03f,  7.26755e-03f, -3.09503e-02f,  9.88580e-01f,  4.87921e-02f, -1.67710e-02f,  5.13372e-03f, -9.26747e-04f }, // 122/128
		{ -9.78093e-04f,  6.09305e-03f, -2.60456e-02f,  9.90917e-01f,  4.04274e-02f, -1.39548e-02f,  4.27773e-03f, -7.72802e-04f }, // 123/128
		{ -7.86031e-04f,  4.90322e-03f, -2.10389e-02f,  9.93082e-01f,  3.21531e-02f, -1.11453e-02f,  3.42130e-03f, -6.18544e-04f }, // 124/128
		{ -5.92100e-04f,  3.69852e-03f, -1.59305e-02f,  9.95074e-01f,  2.39714e-02f, -8.34364e-03f,  2.56486e-03f, -4.64053e-04f }, // 125/128
		{ -3.96391e-04f,  2.47942e-03f, -1.07209e-02f,  9.96891e-01f,  1.58840e-02f, -5.55134e-03f,  1.70888e-03f, -3.09412e-04f }, // 126/128
		{ -1.98993e-04f,  1.24642e-03f, -5.41054e-03f,  9.98534e-01f,  7.89295e-03f, -2.76968e-03f,  8.53777e-04f, -1.54700e-04f }, // 127/128
		{  0.00000e+00f,  0.00000e+00f,  0.00000e+00f,  1.00000e+00f,  0.00000e+00f,  0.00000e+00f,  0.00000e+00f,  0.00000e+00f }, // 128/128
	};
	
    /* Instrumentation Taps */
	private static final String INSTRUMENT_SYMBOL_SPREAD = "Tap Point: Symbol Spread (Goal=2.0)";
	private FloatTap mSymbolSpreadTap;
    private List<TapGroup> mAvailableTaps;
	
	private static final int NUMBER_FILTER_TAPS = 8;
	private static final int NUMBER_FILTER_STEPS = 128;
	
	private static final int SAMPLE_RATE = 48000;
	private static final int SYMBOL_RATE = 4800;
	
	/* Tracking loop gain constant */
	private static final double K_SYMBOL_SPREAD = 0.0100;

	/* Constraints on symbol spreading */
	private static final float SYMBOL_SPREAD_MAX = 2.4f; // upper range limit: +20%
	private static final float SYMBOL_SPREAD_MIN = 1.6f; // lower range limit: -20%

	/* Symbol clock tracking loop gain */
	private static final double K_SYMBOL_TIMING = 0.025;
	
	/* Coarse and fine frequency tracking constants */
	private static final double K_COARSE_FREQUENCY = 0.00125;
	private static final double K_FINE_FREQUENCY = 0.125;
	
	/* Frequency correction broadcast threshold */
//	private static final double COARSE_FREQUENCY_DEADBAND = 1.66;
	private static final double COARSE_FREQUENCY_THRESHOLD = 1.20;
	
	/* 2.0 symbol spread gives -3, -1, 1, 3 */
	private float mSymbolSpread = 2.0f;
	private float mSymbolClock = 0.0f;
	private float mSymbolTime = (float)SYMBOL_RATE / (float)SAMPLE_RATE;
	
	private float mFineFrequencyCorrection = 0.0f;
	private float mCoarseFrequencyCorrection = 0.0f;

	private float mHistory[] = new float[ NUMBER_FILTER_TAPS ];
	private int mHistoryLast = 0;
	
	private RealSampleListener mListener;
	
	private DirectGainControl mGainController = 
			new DirectGainControl( 15.0f, 0.1f, 35.0f, 0.3f );

	private int mFrequencyAdjustmentRequested = 0;
	private int mFrequencyCorrection = 0;
	private int mFrequencyCorrectionMaximum = 3000;
	private boolean mResetFrequencyTracker = false;
	private FrequencyCorrectionProcessor mFrequencyCorrectionProcessor;
	private Listener<FrequencyChangeEvent> mFrequencyChangeListener;
	
	/**
	 * C4FM Symbol Filter
	 * 
	 * Sample Gain values - the gain of incoming sample values will critically
	 * impact the performance of this filter.  Gain should be adjusted to
	 * optimize the value of mSymbolSpread toward a value of 2.0.  If the value
	 * is less than 2.0, then increase gain.  If the value is over 2.0, then
	 * decrease gain.
	 * 
	 * When preceded by the RealAutomaticGainFilter class, an optimal gain
	 * setting for this filter is 15.4 and that yields a symbol spread that
	 * is centered on 2.0, ranging 1.92 to 2.08.
	 */
	public C4FMSymbolFilter( int frequencyCorrectionMaximum )
	{
		mFrequencyCorrectionMaximum = frequencyCorrectionMaximum;
	}
	
	public void dispose()
	{
		mGainController = null;
//		mFrequencyCorrectionControl = null;
		mListener = null;
	}
	
	@Override
	public void receive( RealBuffer buffer )
	{
		for( float sample: buffer.getSamples() )
		{
			receive( sample );
		}

		/* If a frequency correction was requested during the processing of this
		 * buffer, we'll apply the change and it will be reflected in the next 
		 * arriving buffer.  Reset the lock on frequency correction and reset 
		 * the internal frequency correction tracker */
		if( mFrequencyAdjustmentRequested != 0 )
		{
			int correction = mFrequencyCorrection + mFrequencyAdjustmentRequested;
			
			if( correction > mFrequencyCorrectionMaximum )
			{
				correction = mFrequencyCorrectionMaximum;
			}
			else if( correction < -mFrequencyCorrectionMaximum )
			{
				correction = -mFrequencyCorrectionMaximum;
			}

			broadcast( new FrequencyChangeEvent( 
				Event.REQUEST_CHANNEL_FREQUENCY_CORRECTION_CHANGE, correction ) );
			
			mFrequencyAdjustmentRequested = 0;
		}
	}

    public void receive( float sample )
    {
    	sample = mGainController.correct( sample );
    	
		if( mResetFrequencyTracker )
		{
			mCoarseFrequencyCorrection = 0.0f;
			mFineFrequencyCorrection = 0.0f;
			
			broadcast( new FrequencyChangeEvent( 
					Event.REQUEST_CHANNEL_FREQUENCY_CORRECTION_CHANGE, 0 ));
			
			mResetFrequencyTracker = false;
		}
		
		mSymbolClock += mSymbolTime;
		
		mHistory[ mHistoryLast++ ] = sample;
		
		mHistoryLast %= NUMBER_FILTER_TAPS;
		
		if( mSymbolClock > 1.0f )
		{
			mSymbolClock -= 1.0f;
			
			int imu = (int)Math.floor( 0.5 + 
				( (float)NUMBER_FILTER_STEPS * ( mSymbolClock / mSymbolTime ) ) );
			
			if( imu >= NUMBER_FILTER_STEPS )
			{
				imu = NUMBER_FILTER_STEPS - 1;
			}

			int imu_p1 = imu + 1;

			int j = mHistoryLast;
			
			double interp = 0.0;
			double interp_p1 = 0.0;
			
			for( int i = 0; i < NUMBER_FILTER_TAPS; i++ )
			{
				interp += TAPS[ imu][ i ] * mHistory[ j ];
				interp_p1 += TAPS[ imu_p1 ][ i ] * mHistory[ j ];
				
				j = ( j + 1 ) % NUMBER_FILTER_TAPS;
			}

			/* Output symbol will be interpolated value corrected for symbol
			 * spread and frequency offset */
			interp -= mFineFrequencyCorrection;
			interp_p1 -= mFineFrequencyCorrection;

			/* Correct output for symbol deviation (spread) */
			float output = (float)( 2.0 * interp / mSymbolSpread );

			/* Detect received symbol error: basically use a hard decision and
			 * subtract off expected position nominal symbol level which will be
			 * +/- 0.5 * symbol spread and +/- 1.5 symbol spread.  Remember that
			 * nominal symbol spread will be 2.0 */
			double symbolError;
			
			if( interp < -mSymbolSpread )
			{
				/* symbol is -3: Expected at -1.5 * symbol spread */
				symbolError = interp + ( 1.5 * mSymbolSpread );
				mSymbolSpread -= ( symbolError * 0.5 * K_SYMBOL_SPREAD );
			}
			else if( interp < 0.0 ) 
			{
				/* symbol is -1: Expected at -0.5 * symbol_spread */
				symbolError = interp + (0.5 * mSymbolSpread );
				mSymbolSpread -= ( symbolError * K_SYMBOL_SPREAD );
			} 
			else if( interp < mSymbolSpread ) 
			{
				/* symbol is +1: Expected at +0.5 * symbol_spread */
				symbolError = interp - ( 0.5 * mSymbolSpread );
				mSymbolSpread += ( symbolError * K_SYMBOL_SPREAD );
			} 
			else 
			{
				/* symbol is +3: Expected at +1.5 * symbol_spread */
				symbolError = interp - ( 1.5 * mSymbolSpread );
				mSymbolSpread += ( symbolError * 0.5 * K_SYMBOL_SPREAD );
			}

			/* Symbol clock tracking loop adjustment */
			if( interp_p1 < interp )
			{
				mSymbolClock += symbolError * K_SYMBOL_TIMING;
			}
			else
			{
				mSymbolClock -= symbolError * K_SYMBOL_TIMING;
			}
			
			if( mSymbolSpread < SYMBOL_SPREAD_MIN )
			{
				mGainController.increase();
				
				mSymbolSpread = SYMBOL_SPREAD_MIN;
			}
			else if( mSymbolSpread > SYMBOL_SPREAD_MAX )
			{
				mGainController.decrease();

				mSymbolSpread = SYMBOL_SPREAD_MAX;
			}

			mCoarseFrequencyCorrection += ( ( mFineFrequencyCorrection - 
					mCoarseFrequencyCorrection ) * K_COARSE_FREQUENCY );
			
			mFineFrequencyCorrection += ( symbolError * K_FINE_FREQUENCY );
			
			/* Queue a frequency adjustment (once per buffer) as needed */
			if( Math.abs( mCoarseFrequencyCorrection ) > COARSE_FREQUENCY_THRESHOLD )
			{
				mFrequencyAdjustmentRequested = 
						500 * ( mCoarseFrequencyCorrection > 0 ? 1 : -1 );
			}
			
			if( mSymbolSpreadTap != null )
			{
				mSymbolSpreadTap.receive( mSymbolSpread );
			}

			/* dispatch the interpolated value to the listener */
			if( mListener != null )
			{
				mListener.receive( output );
			}
		}
    }

	@Override
    public void setListener( RealSampleListener listener )
    {
		mListener = listener;
    }

	@Override
    public void removeListener( RealSampleListener listener )
    {
		mListener = null;
    }
	
	@Override
    public List<TapGroup> getTapGroups()
    {
		if( mAvailableTaps == null )
		{
			mAvailableTaps = new ArrayList<>();
			
			/*
			 * Since the target is 2.0, we subtract 2.0 from the value so that
			 * a good value is plotted right on the zero center line
			 */
			TapGroup group = new TapGroup( "C4FM Symbol Filter" );
			
			group.add( 
					new AdditiveFloatTap( INSTRUMENT_SYMBOL_SPREAD, 0, 0.1f, -2.0f ) );
			
			mAvailableTaps.add( group );
		}
		
		return mAvailableTaps;
    }

	@Override
    public void registerTap( Tap tap )
    {
		if( tap.getName().contentEquals( INSTRUMENT_SYMBOL_SPREAD ) )
		{
			mSymbolSpreadTap = (FloatTap)tap;
		}
    }

	@Override
    public void unregisterTap( Tap tap )
    {
		if( tap.getName().contentEquals( INSTRUMENT_SYMBOL_SPREAD ) )
		{
			mSymbolSpreadTap = null;
		}
    }

	public void broadcast( FrequencyChangeEvent event )
	{
		if( mFrequencyChangeListener != null )
		{
			mFrequencyChangeListener.receive( event );
		}
	}
	
	public void setFrequencyChangeListener( Listener<FrequencyChangeEvent> listener )
	{
		mFrequencyChangeListener = listener;
	}
	
	@Override
	public Listener<FrequencyChangeEvent> getFrequencyChangeListener()
	{
		if( mFrequencyCorrectionProcessor == null )
		{
			mFrequencyCorrectionProcessor = new FrequencyCorrectionProcessor();
		}
		
		return mFrequencyCorrectionProcessor;
	}

	/**
	 * Receives notifications that the channel frequency correction has been
	 * applied and updates internal tracking value.
	 */
	public class FrequencyCorrectionProcessor implements Listener<FrequencyChangeEvent>
	{
		@Override
		public void receive( FrequencyChangeEvent event )
		{
			switch( event.getEvent() )
			{
				case NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_CHANGE:
					mFrequencyCorrection = event.getValue().intValue();

					//Reset internal frequency tracking
					mCoarseFrequencyCorrection = 0.0f;
					mFineFrequencyCorrection = 0.0f;
					break;
				case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
				case NOTIFICATION_SAMPLE_RATE_CHANGE:
					mResetFrequencyTracker = true;
					break;
				default:
					break;
			}
		}
	}
}
