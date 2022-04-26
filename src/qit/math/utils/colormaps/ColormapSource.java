/*******************************************************************************
 *
 * Quantitative Imaging Toolkit (QIT) (c) 2012-2022 Ryan Cabeen
 * All rights reserved.
 *
 * The Software remains the property of Ryan Cabeen ("the Author").
 *
 * The Software is distributed "AS IS" under this Licence solely for
 * non-commercial use in the hope that it will be useful, but in order
 * that the Author as a charitable foundation protects its assets for
 * the benefit of its educational and research purposes, the Author
 * makes clear that no condition is made or to be implied, nor is any
 * warranty given or to be implied, as to the accuracy of the Software,
 * or that it will be suitable for any particular purpose or for use
 * under any specific conditions. Furthermore, the Author disclaims
 * all responsibility for the use which is made of the Software. It
 * further disclaims any liability for the outcomes arising from using
 * the Software.
 *
 * The Licensee agrees to indemnify the Author and hold the
 * Author harmless from and against any and all claims, damages and
 * liabilities asserted by third parties (including claims for
 * negligence) which arise directly or indirectly from the use of the
 * Software or the sale of any products based on the Software.
 *
 * No part of the Software may be reproduced, modified, transmitted or
 * transferred in any form or by any means, electronic or mechanical,
 * without the express permission of the Author. The permission of
 * the Author is not required if the said reproduction, modification,
 * transmission or transference is done without financial return, the
 * conditions of this Licence are imposed upon the receiver of the
 * product, and all original and amended source code is included in any
 * transmitted product. You may be held legally responsible for any
 * copyright infringement that is caused or encouraged by your failure to
 * abide by these terms and conditions.
 *
 * You are not permitted under this Licence to use this Software
 * commercially. Use for which any financial return is received shall be
 * defined as commercial use, and includes (1) integration of all or part
 * of the source code or the Software into a product for sale or license
 * by or on behalf of Licensee to third parties or (2) use of the
 * Software or any derivative of it for research with the final aim of
 * developing software products for sale or license to a third party or
 * (3) use of the Software or any derivative of it for research with the
 * final aim of developing non-software products for sale or license to a
 * third party, or (4) use of the Software to provide any service to an
 * external organisation for which payment is received.
 *
 ******************************************************************************/

package qit.math.utils.colormaps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import qit.base.Global;
import qit.base.Logging;
import qit.base.utils.PathUtils;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.structs.VectFunction;
import qit.math.source.VectFunctionSource;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ColormapSource
{
    public static final String NAME_FIELD = "name";
    public static final String INDEX_FIELD = "index";
    public static final String RED_FIELD = "r";
    public static final String GREEN_FIELD = "g";
    public static final String BLUE_FIELD = "b";
    public static final String ALPHA_FIELD = "a";

    public static final String GRAYSCALE = "grayscale";
    public static final String DIVERGING = "diverging";
    public static final String COOL = "cool";
    public static final String WARM = "warm";
    public static final String REDGRAD = "redgrad";
    public static final String BLUEGRAD = "bluegrad";
    public static final String TURBO = "turbo";
    public static final String RAINBOW = "rainbow";
    public static final String JET = "jet";
    public static final String JETGRAY = "jetgray";

    public static final String WHITE = "White";
    public static final String RED = "Red";
    public static final String GREEN = "Green";
    public static final String BLUE = "Blue";
    public static final String YELLOW = "Yellow";
    public static final String ORANGE = "Orange";
    public static final String CYAN = "Cyan";
    public static final String MAGENTA = "Magenta";
    public static final String PINK = "Pink";
    public static final String BLACK = "Black";
    public static final String PURPLE = "Purple";
    public static final String VIOLET = "Violet";
    public static final String LAVENDER = "Lavender";
    public static final String MAROON = "Maroon";
    public static final String CORAL = "Coral";
    public static final String SALMON = "Salmon";
    public static final String BROWN = "Brown";
    public static final String WHEAT = "Wheat";
    public static final String CHOCOLATE = "Chocolate";
    public static final String BURNTSIENNA = "BurntSienna";
    public static final String GOLD = "Gold";
    public static final String KHAKI = "Khaki";
    public static final String CHARTREUSE = "Chartreuse";
    public static final String SEAGREEN = "SeaGreen";
    public static final String AQUAMARINE = "Aquamarine";
    public static final String TURQUOISE = "Turquoise";
    public static final String POWDERBLUE = "PowderBlue";
    public static final String STEELBLUE = "SteelBlue";
    public static final String SKYBLUE = "SkyBlue";
    public static final String DODGERBLUE = "DodgerBlue";
    public static final String AZURE = "Azure";
    public static final String IVORY = "Ivory";
    public static final String HONEYDEW = "Honeydew";
    public static final String SEASHELL = "Seashell";
    public static final String BERMUDA = "Bermuda";
    public static final String CANARY = "Canary";
    public static final String SEAGULL = "Seagull";
    public static final String RAJAH = "Rajah";
    public static final String CONIFER = "Conifer";
    public static final String PIGPINK = "PigPink";
    public static final String PUERTO = "Puerto";
    public static final String TANGERINE = "Tangerine";
    public static final String POLO = "Polo";
    public static final String PLUM = "Plum";
    public static final String SUNGLOW = "Sunglow";
    public static final String ELF = "Elf";
    public static final String CHETWODE = "Chetwode";
    public static final String TAWNY = "Tawny";
    public static final String CHERISE = "Cherise";

    private static Map<String, Color> SOLID = Maps.newLinkedHashMap();
    private static Map<String, Table> DISCRETE = Maps.newLinkedHashMap();
    private static Map<String, VectFunction> SCALAR = Maps.newLinkedHashMap();

    private static boolean INITED = false;

    // use the great cool-warm colormap from Ken Moreland
    private static final double[][] COOL_WARM_COLORMAP = {{0, 0.2298057, 0.298717966, 0.753683153}, {0.00390625, 0.234299935, 0.305559204, 0.759874796}, {0.0078125, 0.238810063, 0.312388385, 0.766005866}, {0.01171875, 0.243336663, 0.319205292, 0.772075394}, {0.015625, 0.247880265, 0.326009656, 0.778082421}, {0.01953125, 0.25244136, 0.332801165, 0.784026001}, {0.0234375, 0.257020396, 0.339579464, 0.789905199}, {0.02734375, 0.261617779, 0.346344164, 0.79571909}, {0.03125, 0.26623388, 0.353094838, 0.801466763}, {0.03515625, 0.270869029, 0.359831032, 0.807147315}, {0.0390625, 0.275523523, 0.36655226, 0.812759858}, {0.04296875, 0.28019762, 0.373258014, 0.818303516}, {0.046875, 0.284891546, 0.379947761, 0.823777422}, {0.05078125, 0.289605495, 0.386620945, 0.829180725}, {0.0546875, 0.294339624, 0.393276993, 0.834512584}, {0.05859375, 0.299094064, 0.399915313, 0.839772171}, {0.0625, 0.30386891, 0.406535296, 0.84495867}, {0.06640625, 0.308664231, 0.413136319, 0.850071279}, {0.0703125, 0.313480065, 0.419717745, 0.855109207}, {0.07421875, 0.318316422, 0.426278924, 0.860071679}, {0.078125, 0.323173283, 0.432819194, 0.864957929}, {0.08203125, 0.328050603, 0.439337884, 0.869767207}, {0.0859375, 0.332948312, 0.445834313, 0.874498775}, {0.08984375, 0.337866311, 0.45230779, 0.87915191}, {0.09375, 0.342804478, 0.458757618, 0.883725899}, {0.09765625, 0.347762667, 0.465183092, 0.888220047}, {0.1015625, 0.352740705, 0.471583499, 0.892633669}, {0.10546875, 0.357738399, 0.477958123, 0.896966095}, {0.109375, 0.362755532, 0.484306241, 0.90121667}, {0.11328125, 0.367791863, 0.490627125, 0.905384751}, {0.1171875, 0.372847134, 0.496920043, 0.909469711}, {0.12109375, 0.37792106, 0.503184261, 0.913470934}, {0.125, 0.38301334, 0.50941904, 0.917387822}, {0.12890625, 0.38812365, 0.515623638, 0.921219788}, {0.1328125, 0.39325165, 0.521797312, 0.924966262}, {0.13671875, 0.398396976, 0.527939316, 0.928626686}, {0.140625, 0.40355925, 0.534048902, 0.932200518}, {0.14453125, 0.408738074, 0.540125323, 0.93568723}, {0.1484375, 0.413933033, 0.546167829, 0.939086309}, {0.15234375, 0.419143694, 0.552175668, 0.942397257}, {0.15625, 0.424369608, 0.558148092, 0.945619588}, {0.16015625, 0.429610311, 0.564084349, 0.948752835}, {0.1640625, 0.434865321, 0.56998369, 0.951796543}, {0.16796875, 0.440134144, 0.575845364, 0.954750272}, {0.171875, 0.445416268, 0.581668623, 0.957613599}, {0.17578125, 0.450711169, 0.587452719, 0.960386113}, {0.1796875, 0.456018308, 0.593196905, 0.96306742}, {0.18359375, 0.461337134, 0.598900436, 0.96565714}, {0.1875, 0.46666708, 0.604562568, 0.968154911}, {0.19140625, 0.472007569, 0.61018256, 0.970560381}, {0.1953125, 0.477358011, 0.615759672, 0.972873218}, {0.19921875, 0.482717804, 0.621293167, 0.975093102}, {0.203125, 0.488086336, 0.626782311, 0.97721973}, {0.20703125, 0.493462982, 0.632226371, 0.979252813}, {0.2109375, 0.498847107, 0.637624618, 0.981192078}, {0.21484375, 0.504238066, 0.642976326, 0.983037268}, {0.21875, 0.509635204, 0.648280772, 0.98478814}, {0.22265625, 0.515037856, 0.653537236, 0.986444467}, {0.2265625, 0.520445349, 0.658745003, 0.988006036}, {0.23046875, 0.525857, 0.66390336, 0.989472652}, {0.234375, 0.531272118, 0.669011598, 0.990844132}, {0.23828125, 0.536690004, 0.674069012, 0.99212031}, {0.2421875, 0.542109949, 0.679074903, 0.993301037}, {0.24609375, 0.54753124, 0.684028574, 0.994386177}, {0.25, 0.552953156, 0.688929332, 0.995375608}, {0.25390625, 0.558374965, 0.693776492, 0.996269227}, {0.2578125, 0.563795935, 0.698569369, 0.997066945}, {0.26171875, 0.569215322, 0.703307287, 0.997768685}, {0.265625, 0.574632379, 0.707989572, 0.99837439}, {0.26953125, 0.580046354, 0.712615557, 0.998884016}, {0.2734375, 0.585456486, 0.717184578, 0.999297533}, {0.27734375, 0.590862011, 0.721695979, 0.999614929}, {0.28125, 0.596262162, 0.726149107, 0.999836203}, {0.28515625, 0.601656165, 0.730543315, 0.999961374}, {0.2890625, 0.607043242, 0.734877964, 0.999990472}, {0.29296875, 0.61242261, 0.739152418, 0.999923544}, {0.296875, 0.617793485, 0.743366047, 0.999760652}, {0.30078125, 0.623155076, 0.747518228, 0.999501871}, {0.3046875, 0.628506592, 0.751608345, 0.999147293}, {0.30859375, 0.633847237, 0.755635786, 0.998697024}, {0.3125, 0.639176211, 0.759599947, 0.998151185}, {0.31640625, 0.644492714, 0.763500228, 0.99750991}, {0.3203125, 0.649795942, 0.767336039, 0.996773351}, {0.32421875, 0.655085089, 0.771106793, 0.995941671}, {0.328125, 0.660359348, 0.774811913, 0.995015049}, {0.33203125, 0.665617908, 0.778450826, 0.993993679}, {0.3359375, 0.670859959, 0.782022968, 0.992877768}, {0.33984375, 0.676084688, 0.78552778, 0.991667539}, {0.34375, 0.681291281, 0.788964712, 0.990363227}, {0.34765625, 0.686478925, 0.792333219, 0.988965083}, {0.3515625, 0.691646803, 0.795632765, 0.987473371}, {0.35546875, 0.696794099, 0.798862821, 0.985888369}, {0.359375, 0.701919999, 0.802022864, 0.984210369}, {0.36328125, 0.707023684, 0.805112381, 0.982439677}, {0.3671875, 0.712104339, 0.808130864, 0.980576612}, {0.37109375, 0.717161148, 0.811077814, 0.978621507}, {0.375, 0.722193294, 0.813952739, 0.976574709}, {0.37890625, 0.727199962, 0.816755156, 0.974436577}, {0.3828125, 0.732180337, 0.81948459, 0.972207484}, {0.38671875, 0.737133606, 0.82214057, 0.969887816}, {0.390625, 0.742058956, 0.824722639, 0.967477972}, {0.39453125, 0.746955574, 0.827230344, 0.964978364}, {0.3984375, 0.751822652, 0.829663241, 0.962389418}, {0.40234375, 0.756659379, 0.832020895, 0.959711569}, {0.40625, 0.761464949, 0.834302879, 0.956945269}, {0.41015625, 0.766238556, 0.836508774, 0.95409098}, {0.4140625, 0.770979397, 0.838638169, 0.951149176}, {0.41796875, 0.775686671, 0.840690662, 0.948120345}, {0.421875, 0.780359577, 0.842665861, 0.945004985}, {0.42578125, 0.78499732, 0.84456338, 0.941803607}, {0.4296875, 0.789599105, 0.846382843, 0.938516733}, {0.43359375, 0.79416414, 0.848123884, 0.935144898}, {0.4375, 0.798691636, 0.849786142, 0.931688648}, {0.44140625, 0.803180808, 0.85136927, 0.928148539}, {0.4453125, 0.807630872, 0.852872925, 0.92452514}, {0.44921875, 0.812041048, 0.854296776, 0.92081903}, {0.453125, 0.81641056, 0.855640499, 0.917030798}, {0.45703125, 0.820738635, 0.856903782, 0.913161047}, {0.4609375, 0.825024503, 0.85808632, 0.909210387}, {0.46484375, 0.829267397, 0.859187816, 0.90517944}, {0.46875, 0.833466556, 0.860207984, 0.901068838}, {0.47265625, 0.837621221, 0.861146547, 0.896879224}, {0.4765625, 0.841730637, 0.862003236, 0.892611249}, {0.48046875, 0.845794055, 0.862777795, 0.888265576}, {0.484375, 0.849810727, 0.863469972, 0.883842876}, {0.48828125, 0.853779913, 0.864079527, 0.87934383}, {0.4921875, 0.857700874, 0.864606232, 0.874769128}, {0.49609375, 0.861572878, 0.865049863, 0.870119469}, {0.5, 0.865395197, 0.86541021, 0.865395561}, {0.50390625, 0.86977749, 0.863633958, 0.859948576}, {0.5078125, 0.874064226, 0.861776352, 0.854466231}, {0.51171875, 0.878255583, 0.859837644, 0.848949435}, {0.515625, 0.882351728, 0.857818097, 0.843399101}, {0.51953125, 0.886352818, 0.85571798, 0.837816138}, {0.5234375, 0.890259, 0.853537573, 0.832201453}, {0.52734375, 0.89407041, 0.851277164, 0.826555954}, {0.53125, 0.897787179, 0.848937047, 0.820880546}, {0.53515625, 0.901409427, 0.846517528, 0.815176131}, {0.5390625, 0.904937269, 0.844018919, 0.809443611}, {0.54296875, 0.908370816, 0.841441541, 0.803683885}, {0.546875, 0.911710171, 0.838785722, 0.79789785}, {0.55078125, 0.914955433, 0.836051799, 0.792086401}, {0.5546875, 0.918106696, 0.833240115, 0.786250429}, {0.55859375, 0.921164054, 0.830351023, 0.780390824}, {0.5625, 0.924127593, 0.827384882, 0.774508472}, {0.56640625, 0.926997401, 0.824342058, 0.768604257}, {0.5703125, 0.929773562, 0.821222926, 0.76267906}, {0.57421875, 0.932456159, 0.818027865, 0.756733758}, {0.578125, 0.935045272, 0.814757264, 0.750769226}, {0.58203125, 0.937540984, 0.811411517, 0.744786333}, {0.5859375, 0.939943375, 0.807991025, 0.738785947}, {0.58984375, 0.942252526, 0.804496196, 0.732768931}, {0.59375, 0.944468518, 0.800927443, 0.726736146}, {0.59765625, 0.946591434, 0.797285187, 0.720688446}, {0.6015625, 0.948621357, 0.793569853, 0.714626683}, {0.60546875, 0.950558373, 0.789781872, 0.708551706}, {0.609375, 0.952402567, 0.785921682, 0.702464356}, {0.61328125, 0.954154029, 0.781989725, 0.696365473}, {0.6171875, 0.955812849, 0.777986449, 0.690255891}, {0.62109375, 0.957379123, 0.773912305, 0.68413644}, {0.625, 0.958852946, 0.769767752, 0.678007945}, {0.62890625, 0.960234418, 0.765553251, 0.671871226}, {0.6328125, 0.961523642, 0.761269267, 0.665727098}, {0.63671875, 0.962720725, 0.756916272, 0.659576372}, {0.640625, 0.963825777, 0.752494738, 0.653419853}, {0.64453125, 0.964838913, 0.748005143, 0.647258341}, {0.6484375, 0.965760251, 0.743447967, 0.64109263}, {0.65234375, 0.966589914, 0.738823693, 0.634923509}, {0.65625, 0.96732803, 0.734132809, 0.628751763}, {0.66015625, 0.967974729, 0.729375802, 0.62257817}, {0.6640625, 0.96853015, 0.724553162, 0.616403502}, {0.66796875, 0.968994435, 0.719665383, 0.610228525}, {0.671875, 0.969367729, 0.714712956, 0.604054002}, {0.67578125, 0.969650186, 0.709696378, 0.597880686}, {0.6796875, 0.969841963, 0.704616143, 0.591709328}, {0.68359375, 0.969943224, 0.699472746, 0.585540669}, {0.6875, 0.969954137, 0.694266682, 0.579375448}, {0.69140625, 0.969874878, 0.688998447, 0.573214394}, {0.6953125, 0.969705626, 0.683668532, 0.567058232}, {0.69921875, 0.96944657, 0.678277431, 0.560907681}, {0.703125, 0.969097901, 0.672825633, 0.554763452}, {0.70703125, 0.968659818, 0.667313624, 0.54862625}, {0.7109375, 0.968132528, 0.661741889, 0.542496774}, {0.71484375, 0.967516241, 0.656110908, 0.536375716}, {0.71875, 0.966811177, 0.650421156, 0.530263762}, {0.72265625, 0.966017559, 0.644673104, 0.524161591}, {0.7265625, 0.965135621, 0.638867216, 0.518069875}, {0.73046875, 0.964165599, 0.63300395, 0.511989279}, {0.734375, 0.963107739, 0.627083758, 0.505920462}, {0.73828125, 0.961962293, 0.621107082, 0.499864075}, {0.7421875, 0.960729521, 0.615074355, 0.493820764}, {0.74609375, 0.959409687, 0.608986, 0.487791167}, {0.75, 0.958003065, 0.602842431, 0.481775914}, {0.75390625, 0.956509936, 0.596644046, 0.475775629}, {0.7578125, 0.954930586, 0.590391232, 0.46979093}, {0.76171875, 0.95326531, 0.584084361, 0.463822426}, {0.765625, 0.951514411, 0.57772379, 0.457870719}, {0.76953125, 0.949678196, 0.571309856, 0.451936407}, {0.7734375, 0.947756983, 0.564842879, 0.446020077}, {0.77734375, 0.945751096, 0.558323158, 0.440122312}, {0.78125, 0.943660866, 0.551750968, 0.434243684}, {0.78515625, 0.941486631, 0.545126562, 0.428384763}, {0.7890625, 0.939228739, 0.538450165, 0.422546107}, {0.79296875, 0.936887543, 0.531721972, 0.41672827}, {0.796875, 0.934463404, 0.524942147, 0.410931798}, {0.80078125, 0.931956691, 0.518110821, 0.40515723}, {0.8046875, 0.929367782, 0.511228087, 0.399405096}, {0.80859375, 0.92669706, 0.504293997, 0.393675922}, {0.8125, 0.923944917, 0.49730856, 0.387970225}, {0.81640625, 0.921111753, 0.490271735, 0.382288516}, {0.8203125, 0.918197974, 0.483183431, 0.376631297}, {0.82421875, 0.915203996, 0.476043498, 0.370999065}, {0.828125, 0.912130241, 0.468851724, 0.36539231}, {0.83203125, 0.908977139, 0.461607831, 0.359811513}, {0.8359375, 0.905745128, 0.454311462, 0.354257151}, {0.83984375, 0.902434654, 0.446962183, 0.348729691}, {0.84375, 0.89904617, 0.439559467, 0.343229596}, {0.84765625, 0.895580136, 0.43210269, 0.33775732}, {0.8515625, 0.892037022, 0.424591118, 0.332313313}, {0.85546875, 0.888417303, 0.417023898, 0.326898016}, {0.859375, 0.884721464, 0.409400045, 0.321511863}, {0.86328125, 0.880949996, 0.401718425, 0.316155284}, {0.8671875, 0.877103399, 0.393977745, 0.310828702}, {0.87109375, 0.873182178, 0.386176527, 0.305532531}, {0.875, 0.869186849, 0.378313092, 0.300267182}, {0.87890625, 0.865117934, 0.370385535, 0.295033059}, {0.8828125, 0.860975962, 0.362391695, 0.289830559}, {0.88671875, 0.85676147, 0.354329127, 0.284660075}, {0.890625, 0.852475004, 0.346195061, 0.279521991}, {0.89453125, 0.848117114, 0.337986361, 0.27441669}, {0.8984375, 0.843688361, 0.329699471, 0.269344545}, {0.90234375, 0.839189312, 0.32133036, 0.264305927}, {0.90625, 0.834620542, 0.312874446, 0.259301199}, {0.91015625, 0.829982631, 0.304326513, 0.254330723}, {0.9140625, 0.82527617, 0.295680611, 0.249394851}, {0.91796875, 0.820501754, 0.286929926, 0.244493934}, {0.921875, 0.815659988, 0.278066636, 0.239628318}, {0.92578125, 0.810751482, 0.269081721, 0.234798343}, {0.9296875, 0.805776855, 0.259964733, 0.230004348}, {0.93359375, 0.800736732, 0.250703507, 0.225246666}, {0.9375, 0.795631745, 0.24128379, 0.220525627}, {0.94140625, 0.790462533, 0.231688768, 0.215841558}, {0.9453125, 0.785229744, 0.221898442, 0.211194782}, {0.94921875, 0.779934029, 0.211888813, 0.20658562}, {0.953125, 0.774576051, 0.201630762, 0.202014392}, {0.95703125, 0.769156474, 0.191088518, 0.197481414}, {0.9609375, 0.763675975, 0.180217488, 0.192987001}, {0.96484375, 0.758135232, 0.168961101, 0.188531467}, {0.96875, 0.752534934, 0.157246067, 0.184115123}, {0.97265625, 0.746875773, 0.144974956, 0.179738284}, {0.9765625, 0.741158452, 0.132014017, 0.175401259}, {0.98046875, 0.735383675, 0.1181719, 0.171104363}, {0.984375, 0.729552157, 0.103159409, 0.166847907}, {0.98828125, 0.723664618, 0.086504694, 0.162632207}, {0.9921875, 0.717721782, 0.067344036, 0.158457578}, {0.99609375, 0.711724383, 0.043755173, 0.154324339}, {1, 0.705673158, 0.01555616, 0.150232812}};

    public static void init()
    {
        if (INITED)
        {
            return;
        }

        List<File> colormaps = Lists.newArrayList();

        colormaps.add(new File(new File(new File(Global.getRoot(), "share"), "qit"), "colormaps"));
        colormaps.add(new File(new File(System.getProperty("user.home"), ".qit"), "colormaps"));

        {
            SOLID.put(WHITE, Color.WHITE);
            SOLID.put(ELF, new Color(27, 158, 119));
            SOLID.put(CHETWODE, new Color(117, 112, 179));
            SOLID.put(TAWNY, new Color(217, 95, 2));
            SOLID.put(CHERISE, new Color(231, 41, 138));
            SOLID.put(BERMUDA, new Color(141,211,199));
            SOLID.put(CANARY, new Color(255,255,179));
            SOLID.put(LAVENDER, new Color(190,186,218));
            SOLID.put(SALMON, new Color(251,128,114));
            SOLID.put(SEAGULL, new Color(128,177,211));
            SOLID.put(RAJAH, new Color(253,180,98));
            SOLID.put(CONIFER, new Color(179,222,105));
            SOLID.put(PIGPINK, new Color(252,205,229));
            SOLID.put(PUERTO, new Color(102,194,165));
            SOLID.put(TANGERINE, new Color(252,141,98));
            SOLID.put(POLO, new Color(141,160,203));
            SOLID.put(PLUM, new Color(231,138,195));
            SOLID.put(SUNGLOW, new Color(255,217,47));
            SOLID.put(BURNTSIENNA, new Color(215, 138, 75));
            SOLID.put(GOLD, new Color(255, 215, 0));
            SOLID.put(KHAKI, new Color(240, 230, 140));
            SOLID.put(CHARTREUSE, new Color(127, 255, 0));
            SOLID.put(SEAGREEN, new Color(46, 139, 87));
            SOLID.put(AQUAMARINE, new Color(127, 255, 212));
            SOLID.put(TURQUOISE, new Color(64, 224, 208));
            SOLID.put(POWDERBLUE, new Color(176, 224, 230));
            SOLID.put(STEELBLUE, new Color(70, 130, 180));
            SOLID.put(SKYBLUE, new Color(135, 206, 250));
            SOLID.put(DODGERBLUE, new Color(30, 144, 255));
            SOLID.put(AZURE, new Color(240, 255, 255));
            SOLID.put(IVORY, new Color(255, 255, 240));
            SOLID.put(HONEYDEW, new Color(240, 255, 240));
            SOLID.put(SEASHELL, new Color(255, 245, 238));
            SOLID.put(CHOCOLATE, new Color(210, 105, 30));
            SOLID.put(MAROON, new Color(176, 48, 96));
            SOLID.put(CORAL, new Color(255, 127, 80));
            SOLID.put(BROWN, new Color(165, 42, 42));
            SOLID.put(WHEAT, new Color(245, 222, 179));
            SOLID.put(PURPLE, new Color(160, 32, 240));
            SOLID.put(VIOLET, new Color(238, 130, 238));
            SOLID.put(RED, Color.RED);
            SOLID.put(GREEN, Color.GREEN);
            SOLID.put(BLUE, Color.BLUE);
            SOLID.put(YELLOW, Color.YELLOW);
            SOLID.put(ORANGE, Color.ORANGE);
            SOLID.put(CYAN, Color.CYAN);
            SOLID.put(MAGENTA, Color.MAGENTA);
            SOLID.put(PINK, Color.PINK);
            SOLID.put(BLACK, Color.BLACK);

        }

        {
            Table table = new Table();
            table.withField(NAME_FIELD);
            table.withField(INDEX_FIELD);
            table.withField(RED_FIELD);
            table.withField(GREEN_FIELD);
            table.withField(BLUE_FIELD);
            table.withField(ALPHA_FIELD);

            {
                Record record = new Record();
                record.with(NAME_FIELD, "region0");
                record.with(INDEX_FIELD, String.valueOf(0));
                record.with(RED_FIELD, String.valueOf(0));
                record.with(GREEN_FIELD, String.valueOf(0));
                record.with(BLUE_FIELD, String.valueOf(0));
                record.with(ALPHA_FIELD, String.valueOf(255));
                table.addRecord(record);
            }

            int num = 1024;
            for (int i = 0; i < num; i++)
            {
                int index = i + 1;
                float hue = (float) (i / (Math.PI * 2.0));
                float sat = 0.5f;
                float val = 1.0f;
                Color color = Color.getHSBColor(hue, sat, val);
                Record record = new Record();
                record.with(NAME_FIELD, String.format("region%d", index));
                record.with(INDEX_FIELD, String.valueOf(index));
                record.with(RED_FIELD, String.valueOf(color.getRed()));
                record.with(GREEN_FIELD, String.valueOf(color.getGreen()));
                record.with(BLUE_FIELD, String.valueOf(color.getBlue()));
                record.with(ALPHA_FIELD, String.valueOf(color.getAlpha()));
                table.addRecord(record);
            }

            DISCRETE.put("pastel", table);
        }

        {
            Table table = new Table();
            table.withField(NAME_FIELD);
            table.withField(INDEX_FIELD);
            table.withField(RED_FIELD);
            table.withField(GREEN_FIELD);
            table.withField(BLUE_FIELD);
            table.withField(ALPHA_FIELD);

            {
                Record record = new Record();
                record.with(NAME_FIELD, "region0");
                record.with(INDEX_FIELD, String.valueOf(0));
                record.with(RED_FIELD, String.valueOf(0));
                record.with(GREEN_FIELD, String.valueOf(0));
                record.with(BLUE_FIELD, String.valueOf(0));
                record.with(ALPHA_FIELD, String.valueOf(255));
                table.addRecord(record);
            }

            int num = 3000;
            for (int i = 0; i < num; i++)
            {
                int index = i + 1;
                float hue = (float) (i / (Math.PI * 2.0));
                float sat = 0.5f;
                float val = 1.0f;
                Color color = Color.getHSBColor(hue, sat, val);
                Record record = new Record();
                record.with(NAME_FIELD, String.format("region%d", index));
                record.with(INDEX_FIELD, String.valueOf(index));
                record.with(RED_FIELD, String.valueOf(color.getRed()));
                record.with(GREEN_FIELD, String.valueOf(color.getGreen()));
                record.with(BLUE_FIELD, String.valueOf(color.getBlue()));
                record.with(ALPHA_FIELD, String.valueOf(color.getAlpha()));
                table.addRecord(record);
            }

            DISCRETE.put("pastel3k", table);
        }

        {
            Table table = new Table();
            table.withField(NAME_FIELD);
            table.withField(INDEX_FIELD);
            table.withField(RED_FIELD);
            table.withField(GREEN_FIELD);
            table.withField(BLUE_FIELD);
            table.withField(ALPHA_FIELD);

            {
                Record record = new Record();
                record.with(NAME_FIELD, "region0");
                record.with(INDEX_FIELD, String.valueOf(0));
                record.with(RED_FIELD, String.valueOf(255));
                record.with(GREEN_FIELD, String.valueOf(255));
                record.with(BLUE_FIELD, String.valueOf(255));
                record.with(ALPHA_FIELD, String.valueOf(255));
                table.addRecord(record);
            }

            int num = 1024;
            for (int i = 0; i < num; i++)
            {
                int index = i + 1;
                float hue = (float) (i / (Math.PI * 2.0));
                float sat = 0.5f;
                float val = 1.0f;
                Color color = Color.getHSBColor(hue, sat, val);
                Record record = new Record();
                record.with(NAME_FIELD, String.format("region%d", index));
                record.with(INDEX_FIELD, String.valueOf(index));
                record.with(RED_FIELD, String.valueOf(color.getRed()));
                record.with(GREEN_FIELD, String.valueOf(color.getGreen()));
                record.with(BLUE_FIELD, String.valueOf(color.getBlue()));
                record.with(ALPHA_FIELD, String.valueOf(color.getAlpha()));
                table.addRecord(record);
            }

            DISCRETE.put("pastel_whitebg", table);
        }
        {
            for (File colormap : colormaps)
            {
                File cmaps = new File(colormap, "discrete");
                if (cmaps.isDirectory())
                {
                    for (File f : FileUtils.listFiles(cmaps, new String[]{"csv"}, false))
                    {
                        String fn = f.getAbsolutePath();
                        try
                        {
                            String bn = PathUtils.basename(fn);
                            String name = bn.split("\\.")[0];
                            Table table = Table.read(fn);
                            DISCRETE.put(name, table);
                        }
                        catch (Exception e)
                        {
                            Logging.info("warning: failed to read colormap " + fn + ", due to " + e.getMessage());
                        }
                    }
                }
            }
        }
        {
            for (String name : SOLID.keySet())
            {
                Table table = new Table();
                table.withField(NAME_FIELD);
                table.withField(INDEX_FIELD);
                table.withField(RED_FIELD);
                table.withField(GREEN_FIELD);
                table.withField(BLUE_FIELD);
                table.withField(ALPHA_FIELD);

                Color bg = Color.black;
                Color fg = SOLID.get(name);
                {
                    Record record = new Record();
                    record.with(NAME_FIELD, "background");
                    record.with(INDEX_FIELD, 0);
                    record.with(RED_FIELD, String.valueOf(bg.getRed()));
                    record.with(GREEN_FIELD, String.valueOf(bg.getGreen()));
                    record.with(BLUE_FIELD, String.valueOf(bg.getBlue()));
                    record.with(ALPHA_FIELD, String.valueOf(bg.getAlpha()));
                    table.addRecord(record);
                }
                {
                    Record record = new Record();
                    record.with(NAME_FIELD, "foreground");
                    record.with(INDEX_FIELD, 1);
                    record.with(RED_FIELD, String.valueOf(fg.getRed()));
                    record.with(GREEN_FIELD, String.valueOf(fg.getGreen()));
                    record.with(BLUE_FIELD, String.valueOf(fg.getBlue()));
                    record.with(ALPHA_FIELD, String.valueOf(fg.getAlpha()));
                    table.addRecord(record);
                }

                DISCRETE.put(name, table);
            }
        }

        {
            {
                double[] x = {0, 1};
                Vect[] y = {VectSource.create4D(0, 0, 0, 1), VectSource.create4D(1, 1, 1, 1)};
                VectFunction grayscale = VectFunctionSource.linearInterp(x, y);
                SCALAR.put(GRAYSCALE, grayscale);
            }
            {

                double[] x = {0, 1};
                Vect[] y = {VectSource.create4D(1.0, 0, 0, 1), VectSource.create4D(1, 1, 0, 1)};
                VectFunction function = VectFunctionSource.linearInterp(x, y);
                SCALAR.put(REDGRAD, function);
            }
            {
                double[] x = {0, 1};
                Vect[] y = {VectSource.create4D(0, 0, 1, 1), VectSource.create4D(0, 1, 1, 1)};
                VectFunction function = VectFunctionSource.linearInterp(x, y);
                SCALAR.put(BLUEGRAD, function);
            }
            {
                double[][] source = COOL_WARM_COLORMAP;
                int half = source.length / 2;
                double[] xs = new double[half];
                Vect[] ys = new Vect[half];

                for (int i = 0; i < half; i++)
                {
                    double red = source[i][1];
                    double green = source[i][2];
                    double blue = source[i][3];

                    xs[i] = 2 * source[i][0];
                    ys[i] = VectSource.create4D(red, green, blue, 1.0);
                }
                VectFunction cool = VectFunctionSource.linearInterp(xs, ys);
                SCALAR.put(COOL, cool);
            }
            {
                double[][] source = COOL_WARM_COLORMAP;
                int half = source.length / 2;
                double[] xs = new double[half];
                Vect[] ys = new Vect[half];

                for (int i = 0; i < half; i++)
                {
                    double red = source[i + half][1];
                    double green = source[i + half][2];
                    double blue = source[i + half][3];

                    xs[i] = 2 * (1 - source[i + half][0]);
                    ys[i] = VectSource.create4D(red, green, blue, 1.0);
                }
                VectFunction warm = VectFunctionSource.linearInterp(xs, ys);
                SCALAR.put(WARM, warm);
            }
            {
                double[][] source = COOL_WARM_COLORMAP;
                double[] xs = new double[source.length];
                Vect[] ys = new Vect[source.length];

                for (int i = 0; i < source.length; i++)
                {
                    double red = source[i][1];
                    double green = source[i][2];
                    double blue = source[i][3];

                    xs[i] = source[i][0];
                    ys[i] = VectSource.create4D(red, green, blue, 1.0);
                }
                VectFunction diverging = VectFunctionSource.linearInterp(xs, ys);
                SCALAR.put(DIVERGING, diverging);
            }
            {
                // http://imagej.1557.x6.nabble.com/Jet-colormap-LUT-td5003222.html
                int num = 256;
                double[] xs = new double[num];
                Vect[] ys = new Vect[num];

                for (int i = 0; i < num; i++)
                {
                    double i4 = 4 * i / 256.0;
                    double red = Math.min(Math.max(Math.min(i4 - 1.5, -i4 + 4.5), 0), 1);
                    double green = Math.min(Math.max(Math.min(i4 - 0.5, -i4 + 3.5), 0), 1);
                    double blue = Math.min(Math.max(Math.min(i4 + 0.5, -i4 + 2.5), 0), 1);

                    xs[i] = i / (double) (num - 1);
                    ys[i] = VectSource.create4D(red, green, blue, 1.0);
                }
                VectFunction function = VectFunctionSource.linearInterp(xs, ys);
                SCALAR.put(JET, function);
            }
            {
                // http://imagej.1557.x6.nabble.com/Jet-colormap-LUT-td5003222.html
                int num = 256;
                double[] xs = new double[num+2];
                Vect[] ys = new Vect[num+2];

                xs[0] = 0.0;
                ys[0] = VectSource.create4D(0.5, 0.5, 0.5, 1.0);

                for (int i = 0; i < num; i++)
                {
                    double i4 = 4 * i / 256.0;
                    double red = Math.min(Math.max(Math.min(i4 - 1.5, -i4 + 4.5), 0), 1);
                    double green = Math.min(Math.max(Math.min(i4 - 0.5, -i4 + 3.5), 0), 1);
                    double blue = Math.min(Math.max(Math.min(i4 + 0.5, -i4 + 2.5), 0), 1);

                    xs[i+1] = i / (double) (num+1);
                    ys[i+1] = VectSource.create4D(red, green, blue, 1.0);
                }

                xs[num+1] = 1.0;
                ys[num+1] = VectSource.create4D(0.5, 0.5, 0.5, 1.0);

                VectFunction function = VectFunctionSource.linearInterp(xs, ys);
                SCALAR.put(JETGRAY, function);
            }
            {
                // https://observablehq.com/@mbostock/turbo
                // a polynomial approximation from Mike Bostock
                int num = 256;
                double[] xs = new double[num];
                Vect[] ys = new Vect[num];

                for (int i = 0; i < num; i++)
                {
                    double x = i / (double) (num - 1);

                    double r = 34.61 + x * (1172.33 - x * (10793.56 - x * (33300.12 - x * (38394.49 - x * 14825.05))));
                    double g = 23.31 + x * (557.33 + x * (1225.33 - x * (3574.96 - x * (1073.77 + x * 707.56))));
                    double b = 27.2 + x * (3211.1 - x * (15327.97 - x * (27814 - x * (22569.18 - x * 6838.66))));

                    r = Math.max(0, Math.min(1.0, r / 255.0));
                    g = Math.max(0, Math.min(1.0, g / 255.0));
                    b = Math.max(0, Math.min(1.0, b / 255.0));

                    xs[i] = x;
                    ys[i] = VectSource.create4D(r, g, b, 1.0);
                }
                VectFunction function = VectFunctionSource.linearInterp(xs, ys);
                SCALAR.put(TURBO, function);
            }
            {
                int res = 256;
                double[] xs = new double[res];
                Vect[] ys = new Vect[res];

                for (int i = 0; i < res; i++)
                {
                    float f = i / (float) (res - 1);
                    Color c = Color.getHSBColor(f, 1f, 1f);
                    double red = c.getRed() / (double) 255;
                    double green = c.getGreen() / (double) 255;
                    double blue = c.getBlue() / (double) 255;

                    xs[i] = f;
                    ys[i] = VectSource.create4D(red, green, blue, 1.0);
                }
                VectFunction colormap = VectFunctionSource.linearInterp(xs, ys);
                SCALAR.put(RAINBOW, colormap);
            }

            for (File colormap : colormaps)
            {
                File cmaps = new File(colormap, "scalar");
                if (cmaps.isDirectory())
                {
                    // Logging.info("loading scalar colormaps from " + cmaps.getAbsolutePath());
                    for (File f : FileUtils.listFiles(cmaps, new String[]{"lut"}, false))
                    {
                        String fn = f.getAbsolutePath();
                        try
                        {
                            // Logging.info("...loading scalar colormap from " + fn);
                            String bn = PathUtils.basename(fn);
                            String name = bn.split("\\.")[0];

                            byte[] bytes = FileUtils.readFileToByteArray(f);
                            int num = bytes.length / 3;

                            double[] xs = new double[num];
                            Vect[] ys = new Vect[num];

                            for (int i = 0; i < num; i++)
                            {
                                double r = (bytes[0 * num + i] & 255) / 255.0;
                                double g = (bytes[1 * num + i] & 255) / 255.0;
                                double b = (bytes[2 * num + i] & 255) / 255.0;

                                xs[i] = i / (double) (num - 1);
                                ys[i] = VectSource.create4D(r, g, b, 1.0);
                            }

                            VectFunction function = VectFunctionSource.linearInterp(xs, ys);
                            SCALAR.put(name, function);
                        }
                        catch (Exception e)
                        {
                            Logging.info("warning: failed to read colormap " + fn + ", due to " + e.getMessage());
                        }
                    }
                }
            }
        }

        INITED = true;
    }

    public static Set<String> getSolidNames()
    {
        init();
        return SOLID.keySet();
    }

    public static Color getSolidColor(String name)
    {
        init();
        Global.assume(SOLID.containsKey(name), "solid colormap not found:" + name);
        return SOLID.get(name);
    }

    public static ColormapSolid getSolid(String name)
    {
        return new ColormapSolid().withName(name).withColor(vect(getSolidColor(name)));
    }

    public static Set<String> getDiscreteNames()
    {
        init();
        return DISCRETE.keySet();
    }

    public static Table getDiscreteTable(String name)
    {
        init();
        Global.assume(DISCRETE.containsKey(name), "discrete colormap not found:" + name);
        return DISCRETE.get(name);
    }

    public static ColormapDiscrete getDiscrete(String name)
    {
        return ColormapDiscrete.create(getDiscreteTable(name)).withName(name);
    }

    public static Set<String> getScalarNames()
    {
        init();
        return SCALAR.keySet();
    }

    public static VectFunction getScalarFunction(String name)
    {
        init();
        Global.assume(SCALAR.containsKey(name), "scalar colormap not found:" + name);
        return SCALAR.get(name);
    }

    public static ColormapScalar getScalar(String name)
    {
        return new ColormapScalar().withName(name).withColoring(name);
    }

    public static Vect hsv255(Vect rgb)
    {
        int cr = (int) Math.round(rgb.get(0));
        int cg = (int) Math.round(rgb.get(1));
        int cb = (int) Math.round(rgb.get(2));

        float[] hsv = Color.RGBtoHSB(cr, cg, cb, new float[3]);
        float ch = hsv[0];
        float cs = hsv[1];
        float cv = hsv[2];

        // copy the alpha if its there
        Vect out = rgb.copy();
        out.set(0, ch);
        out.set(1, cs);
        out.set(2, cv);

        return out;
    }

    public static Vect hsv(Vect rgb)
    {
        int cr = (int) Math.round(255 * rgb.get(0));
        int cg = (int) Math.round(255 * rgb.get(1));
        int cb = (int) Math.round(255 * rgb.get(2));

        float[] hsv = Color.RGBtoHSB(cr, cg, cb, new float[3]);
        float ch = hsv[0];
        float cs = hsv[1];
        float cv = hsv[2];

        // copy the alpha if its there
        Vect out = rgb.copy();
        out.set(0, ch);
        out.set(1, cs);
        out.set(2, cv);

        return out;
    }

    public static Vect rgb255(Vect hsv)
    {
        float ch = (float) hsv.get(0);
        float cs = (float) hsv.get(1);
        float cv = (float) hsv.get(2);

        Color rgb = new Color(Color.HSBtoRGB(ch, cs, cv));
        double cr = rgb.getRed();
        double cg = rgb.getGreen();
        double cb = rgb.getBlue();

        // copy the alpha if its there
        Vect out = hsv.copy();
        out.set(0, cr);
        out.set(1, cg);
        out.set(2, cb);

        return out;
    }

    public static Vect rgb(Vect hsv)
    {
        float ch = (float) hsv.get(0);
        float cs = (float) hsv.get(1);
        float cv = (float) hsv.get(2);

        Color rgb = new Color(Color.HSBtoRGB(ch, cs, cv));
        double cr = rgb.getRed() / 255.0;
        double cg = rgb.getGreen() / 255.0;
        double cb = rgb.getBlue() / 255.0;

        // copy the alpha if its there
        Vect out = hsv.copy();
        out.set(0, cr);
        out.set(1, cg);
        out.set(2, cb);

        return out;
    }

    public static Vect vect(Color c)
    {
        double r = c.getRed() / 255.0f;
        double g = c.getGreen() / 255.0f;
        double b = c.getBlue() / 255.0f;

        return VectSource.create4D(r, g, b, 1.0);
    }

    public static Vect vect(Color c, double a)
    {
        double r = c.getRed() / 255.0f;
        double g = c.getGreen() / 255.0f;
        double b = c.getBlue() / 255.0f;

        return VectSource.create4D(r, g, b, a);
    }

    public static Color color(Vect v)
    {
        float r = (float) v.get(0);
        float g = (float) v.get(1);
        float b = (float) v.get(2);

        return new Color(r, g, b);
    }

    public static List<Vect> random(int num)
    {
        List<Vect> out = Lists.newArrayList();
        for (int i = 0; i < num; i++)
        {
            float hue = (float) (i / (Math.PI * 2.0));
            float sat = 0.25f;
            float val = 1.0f;
            Color color = Color.getHSBColor(hue, sat, val);
            out.add(vect(color));
        }
        return out;
    }
}
